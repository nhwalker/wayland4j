package org.wayland4j.client.internal;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.wayland4j.client.OpaqueProxy;
import org.wayland4j.client.Proxy;

/**
 * Single process-wide upcall stub registered with libwayland via
 * {@code wl_proxy_add_dispatcher}. Decodes {@code wl_argument} slots based on
 * the {@code wl_message} signature and routes to the per-class
 * {@link DispatchTable}.
 */
public final class Dispatcher {

    private static final FunctionDescriptor DISPATCHER_DESC = FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,    // user_data
            ValueLayout.ADDRESS,    // target wl_proxy*
            ValueLayout.JAVA_INT,   // opcode
            ValueLayout.ADDRESS,    // wl_message *
            ValueLayout.ADDRESS     // wl_argument *
    );

    private static final ThreadLocal<Boolean> IN_DISPATCH = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final AtomicReference<Throwable> PENDING = new AtomicReference<>();

    private static final MemorySegment DISPATCHER_STUB = createUpcallStub();

    private Dispatcher() {
    }

    public static MemorySegment stub() {
        return DISPATCHER_STUB;
    }

    public static boolean inDispatch() {
        return IN_DISPATCH.get();
    }

    public static Throwable takePending() {
        return PENDING.getAndSet(null);
    }

    /**
     * Install our process-wide dispatcher on a freshly-wrapped proxy and remember
     * the user listener (may be {@code null} until a setListener call).
     */
    public static void install(Proxy proxy, Object listener) {
        ProxyRegistry.Entry entry = ProxyRegistry.register(proxy);
        entry.listener = listener;
        try {
            int rc = (int) NativeLibrary.WL_PROXY_ADD_DISPATCHER.invokeExact(
                    proxy.ptr(),
                    DISPATCHER_STUB,
                    MemorySegment.NULL,
                    MemorySegment.NULL);
            if (rc != 0) {
                throw new IllegalStateException("wl_proxy_add_dispatcher failed: rc=" + rc);
            }
        } catch (Throwable t) {
            sneakyThrow(t);
        }
    }

    private static MemorySegment createUpcallStub() {
        try {
            MethodHandle target = MethodHandles.lookup().findStatic(
                    Dispatcher.class, "onDispatch",
                    MethodType.methodType(int.class,
                            MemorySegment.class, MemorySegment.class, int.class, MemorySegment.class, MemorySegment.class));
            return NativeLibrary.LINKER.upcallStub(target, DISPATCHER_DESC, NativeLibrary.LIB_ARENA);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unused") // called via upcall stub
    private static int onDispatch(
            MemorySegment userData,
            MemorySegment target,
            int opcode,
            MemorySegment messagePtr,
            MemorySegment argsPtr) {
        IN_DISPATCH.set(Boolean.TRUE);
        try {
            long targetAddr = target.address();
            ProxyRegistry.Entry entry = ProxyRegistry.lookup(targetAddr);
            if (entry == null) {
                // Unknown proxy — no listener installed. Drop the event.
                return 0;
            }
            Proxy proxy = entry.proxy;
            Object listener = entry.listener;
            DispatchTable table = Wayland.infoFor(proxy.getClass()).dispatchTable();
            DispatchTable.EventDispatcher d = table.dispatcherFor(opcode);

            // Read wl_message at messagePtr: { name, signature, types[] }
            MemorySegment msg = messagePtr.reinterpret(24);
            MemorySegment sigSeg = msg.get(ValueLayout.ADDRESS, 8L).reinterpret(Long.MAX_VALUE);
            MemorySegment typesArr = msg.get(ValueLayout.ADDRESS, 16L);
            String signature = sigSeg.getString(0, StandardCharsets.UTF_8);

            Object[] decoded = decodeArgs(signature, typesArr, argsPtr, listener);
            if (listener != null) {
                d.dispatch(proxy, listener, decoded);
            }
            if (table.isDestructor(opcode)) {
                // libwayland frees the proxy struct after a destructor event; drop our registry entry.
                ProxyRegistry.unregister(targetAddr);
            }
            return 0;
        } catch (Throwable t) {
            PENDING.compareAndSet(null, t);
            return 0;
        } finally {
            IN_DISPATCH.set(Boolean.FALSE);
        }
    }

    private static Object[] decodeArgs(String signature, MemorySegment typesArr, MemorySegment argsPtr, Object listener) {
        // Skip leading "since" digits.
        int i = 0;
        while (i < signature.length() && Character.isDigit(signature.charAt(i))) i++;

        // Count slots first to size the args memory window.
        int slotCount = 0;
        for (int j = i; j < signature.length(); j++) {
            char c = signature.charAt(j);
            if (c != '?') slotCount++;
        }

        MemorySegment args = argsPtr.reinterpret(WlArgumentLayout.SLOT_SIZE * slotCount);
        Object[] out = new Object[slotCount];
        int slot = 0;
        for (int j = i; j < signature.length(); j++) {
            char c = signature.charAt(j);
            if (c == '?') continue;
            switch (c) {
                case 'i', 'u', 'h' -> out[slot] = WlArgumentLayout.readInt(args, slot);
                case 'f' -> out[slot] = WlArgumentLayout.readInt(args, slot);   // raw fixed; consumer can convert
                case 's' -> out[slot] = WlArgumentLayout.readString(args, slot);
                case 'a' -> out[slot] = WlArgumentLayout.readArray(args, slot);
                case 'o' -> out[slot] = decodeObject(args, slot, typesArr, slot);
                case 'n' -> out[slot] = decodeNewId(args, slot, typesArr, slot);
                default -> throw new IllegalStateException("unsupported signature char: " + c);
            }
            slot++;
        }
        return out;
    }

    private static Proxy decodeObject(MemorySegment args, int slot, MemorySegment typesArr, int typeIndex) {
        long addr = WlArgumentLayout.readAddress(args, slot);
        if (addr == 0L) return null;
        Proxy known = ProxyRegistry.proxyAt(addr);
        if (known != null) return known;
        // For typed 'o' slots the listener parameter has a specific generated type, so
        // returning an OpaqueProxy would CCE inside the dispatch lambda. Only fall back
        // to OpaqueProxy when the slot is untyped (the wl_display.error.object_id case),
        // where the listener parameter is the abstract Proxy type.
        if (slotIsTyped(typesArr, typeIndex)) {
            return null;
        }
        // Don't register; libwayland owns the lifetime and the address may be reused.
        return new OpaqueProxy(MemorySegment.ofAddress(addr));
    }

    private static boolean slotIsTyped(MemorySegment typesArr, int typeIndex) {
        if (typesArr == null || typesArr.address() == 0L) return false;
        MemorySegment window = typesArr.reinterpret((long) (typeIndex + 1) * 8L);
        MemorySegment entry = window.get(ValueLayout.ADDRESS, (long) typeIndex * 8L);
        return entry.address() != 0L;
    }

    private static Proxy decodeNewId(MemorySegment args, int slot, MemorySegment typesArr, int typeIndex) {
        long addr = WlArgumentLayout.readAddress(args, slot);
        if (addr == 0L) return null;
        Proxy known = ProxyRegistry.proxyAt(addr);
        if (known != null) return known;
        // Need to construct a Java wrapper. Use the wl_interface pointer for this slot to find the class.
        if (typesArr == null || typesArr.address() == 0L) return null;
        MemorySegment typesWindow = typesArr.reinterpret((long) (typeIndex + 1) * 8L);
        MemorySegment ifacePtr = typesWindow.get(ValueLayout.ADDRESS, (long) typeIndex * 8L);
        if (ifacePtr.address() == 0L) return null;
        MemorySegment ifaceWindow = ifacePtr.reinterpret(WlInterfaceArena.WL_INTERFACE.byteSize());
        MemorySegment nameStr = ifaceWindow.get(ValueLayout.ADDRESS, 0L).reinterpret(Long.MAX_VALUE);
        String wlName = nameStr.getString(0, StandardCharsets.UTF_8);
        Wayland.ClassInfo info = Wayland.infoForName(wlName);
        Proxy p = info.constructor().apply(MemorySegment.ofAddress(addr));
        ProxyRegistry.register(p);
        Dispatcher.install(p, null);
        return p;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }
}
