package org.wayland4j.client;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import org.wayland4j.client.internal.Dispatcher;
import org.wayland4j.client.internal.NativeLibrary;
import org.wayland4j.client.internal.ProxyRegistry;
import org.wayland4j.client.internal.Wayland;
import org.wayland4j.client.internal.WlArgumentLayout;

/**
 * Base class for every libwayland-client proxy wrapper. Generated proxies in
 * {@code org.wayland4j.client.protocol} extend this and use the protected
 * {@code send*} helpers to invoke {@code wl_proxy_marshal_array_flags}.
 */
public abstract class Proxy {

    private final MemorySegment ptr;

    protected Proxy(MemorySegment ptr) {
        if (ptr == null || ptr.address() == 0L) {
            throw new IllegalArgumentException("null wl_proxy");
        }
        this.ptr = ptr.reinterpret(Long.MAX_VALUE);
    }

    public final MemorySegment ptr() {
        return ptr;
    }

    public final long address() {
        return ptr.address();
    }

    public final int id() {
        try {
            return (int) NativeLibrary.WL_PROXY_GET_ID.invokeExact(ptr);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public final int version() {
        try {
            return (int) NativeLibrary.WL_PROXY_GET_VERSION.invokeExact(ptr);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public final String wlClassName() {
        try {
            MemorySegment p = (MemorySegment) NativeLibrary.WL_PROXY_GET_CLASS.invokeExact(ptr);
            if (p.address() == 0L) return null;
            return p.reinterpret(Long.MAX_VALUE).getString(0, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Route subsequent events for this proxy to {@code queue}. Pass {@code null}
     * to send events back to the display's default queue.
     */
    public final void setQueue(EventQueue queue) {
        MemorySegment qPtr = queue == null ? MemorySegment.NULL : queue.ptr();
        try {
            NativeLibrary.WL_PROXY_SET_QUEUE.invokeExact(ptr, qPtr);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + id() + ", v=" + version() + "]";
    }

    // --- helpers for generated code ---------------------------------------

    /**
     * Send a non-constructor request. {@code signature} is the libwayland
     * signature for this request (without the "since" prefix); {@code args}
     * supplies one Java value per signature character, in order, ignoring
     * {@code ?} modifiers.
     */
    protected static void sendVoid(Proxy self, int opcode, int flags, String signature, Object... args) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment argv = buildArgs(arena, signature, args, /*newIdSlot*/-1);
            MemorySegment res = (MemorySegment) NativeLibrary.WL_PROXY_MARSHAL_ARRAY_FLAGS.invokeExact(
                    self.ptr,
                    opcode,
                    MemorySegment.NULL,
                    0,
                    flags,
                    argv);
            if ((flags & NativeLibrary.WL_MARSHAL_FLAG_DESTROY) != 0) {
                ProxyRegistry.unregister(self.address());
            }
            // res is unused for non-constructor requests
            if (res == null) {
                // suppress warning
            }
        } catch (Throwable t) {
            throwUnchecked(t);
        }
    }

    /** Send a destructor request, removing this proxy from the registry. */
    protected static void sendDestroy(Proxy self, int opcode, String signature, Object... args) {
        sendVoid(self, opcode, NativeLibrary.WL_MARSHAL_FLAG_DESTROY, signature, args);
    }

    /** Send a request whose signature ends in a typed {@code new_id}. */
    protected static <P extends Proxy> P sendConstructor(
            Proxy self, int opcode, int flags, Class<P> proxyClass, String signature, Object... args) {
        Wayland.ClassInfo info = Wayland.infoFor(proxyClass);
        // Find the new_id slot index: count slots up to the 'n' char in the signature.
        int slot = 0;
        int newIdSlot = -1;
        for (int i = sigBodyStart(signature); i < signature.length(); i++) {
            char c = signature.charAt(i);
            if (c == '?') continue;
            if (c == 'n') {
                newIdSlot = slot;
                break;
            }
            slot++;
        }
        if (newIdSlot < 0) {
            throw new IllegalArgumentException("constructor signature missing 'n': " + signature);
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment argv = buildArgs(arena, signature, args, newIdSlot);
            MemorySegment newProxy = (MemorySegment) NativeLibrary.WL_PROXY_MARSHAL_ARRAY_FLAGS.invokeExact(
                    self.ptr,
                    opcode,
                    info.ifaceSegment(),
                    self.version(),
                    flags,
                    argv);
            if (newProxy == null || newProxy.address() == 0L) {
                throw new IllegalStateException("wl_proxy_marshal_array_flags returned null for "
                        + proxyClass.getSimpleName());
            }
            P wrapped = proxyClass.cast(info.constructor().apply(newProxy));
            ProxyRegistry.register(wrapped);
            Dispatcher.install(wrapped, null);
            return wrapped;
        } catch (Throwable t) {
            throwUnchecked(t);
            return null;
        }
    }

    /**
     * Special-case for {@code wl_registry.bind}: dynamic new_id without a fixed
     * interface. Wire signature is {@code "usun"}: name, interface_name,
     * version, new_id.
     */
    protected static <P extends Proxy> P sendBind(
            Proxy self, int opcode, int name, Class<P> proxyClass, int requestedVersion) {
        Wayland.ClassInfo info = Wayland.infoFor(proxyClass);
        int version = Math.min(requestedVersion, info.interfaceVersion());
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment argv = arena.allocate(WlArgumentLayout.SLOT_SIZE * 4, 8);
            argv.fill((byte) 0);
            WlArgumentLayout.writeInt(argv, 0, name);
            MemorySegment ifNameC = allocateUtf8(arena, info.wlName());
            WlArgumentLayout.writeAddress(argv, 1, ifNameC);
            WlArgumentLayout.writeInt(argv, 2, version);
            WlArgumentLayout.writeAddress(argv, 3, MemorySegment.NULL);

            MemorySegment newProxy = (MemorySegment) NativeLibrary.WL_PROXY_MARSHAL_ARRAY_FLAGS.invokeExact(
                    self.ptr,
                    opcode,
                    info.ifaceSegment(),
                    version,
                    0,
                    argv);
            if (newProxy == null || newProxy.address() == 0L) {
                throw new IllegalStateException("bind returned null for " + proxyClass.getSimpleName());
            }
            P wrapped = proxyClass.cast(info.constructor().apply(newProxy));
            ProxyRegistry.register(wrapped);
            Dispatcher.install(wrapped, null);
            return wrapped;
        } catch (Throwable t) {
            throwUnchecked(t);
            return null;
        }
    }

    private static MemorySegment buildArgs(Arena arena, String signature, Object[] args, int newIdSlot) {
        // Count slots, skipping leading "since" digits and '?' modifiers.
        int bodyStart = sigBodyStart(signature);
        int slotCount = 0;
        for (int i = bodyStart; i < signature.length(); i++) {
            if (signature.charAt(i) != '?') slotCount++;
        }
        MemorySegment argv = arena.allocate(WlArgumentLayout.SLOT_SIZE * Math.max(slotCount, 1), 8);
        argv.fill((byte) 0);

        int slot = 0;
        int argIdx = 0;
        for (int i = bodyStart; i < signature.length(); i++) {
            char c = signature.charAt(i);
            if (c == '?') continue;
            if (slot == newIdSlot) {
                // libwayland fills this in; we leave NULL.
                slot++;
                continue;
            }
            Object v = (argIdx < args.length) ? args[argIdx++] : null;
            switch (c) {
                case 'i', 'u', 'h' -> WlArgumentLayout.writeInt(argv, slot, ((Number) v).intValue());
                case 'f' -> WlArgumentLayout.writeInt(argv, slot,
                        WlArgumentLayout.doubleToFixed(((Number) v).doubleValue()));
                case 's' -> WlArgumentLayout.writeAddress(argv, slot,
                        v == null ? MemorySegment.NULL : allocateUtf8(arena, (String) v));
                case 'o' -> WlArgumentLayout.writeAddress(argv, slot,
                        v == null ? MemorySegment.NULL : ((Proxy) v).ptr);
                case 'a' -> WlArgumentLayout.writeAddress(argv, slot,
                        v == null ? MemorySegment.NULL : allocateWlArray(arena, (byte[]) v));
                case 'n' -> {
                    // Static-typed new_id: leave NULL; libwayland fills it.
                    WlArgumentLayout.writeAddress(argv, slot, MemorySegment.NULL);
                }
                default -> throw new IllegalStateException("unsupported signature char in request: " + c);
            }
            slot++;
        }
        return argv;
    }

    static MemorySegment allocateUtf8(Arena arena, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        MemorySegment seg = arena.allocate(bytes.length + 1L, 1);
        MemorySegment.copy(bytes, 0, seg, ValueLayout.JAVA_BYTE, 0, bytes.length);
        seg.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0);
        return seg;
    }

    private static MemorySegment allocateWlArray(Arena arena, byte[] data) {
        // struct wl_array { size_t size; size_t alloc; void *data; }
        MemorySegment header = arena.allocate(24, 8);
        MemorySegment buf = arena.allocate(Math.max(data.length, 1), 1);
        if (data.length > 0) {
            MemorySegment.copy(data, 0, buf, ValueLayout.JAVA_BYTE, 0, data.length);
        }
        header.set(ValueLayout.JAVA_LONG, 0, data.length);
        header.set(ValueLayout.JAVA_LONG, 8, data.length);
        header.set(ValueLayout.ADDRESS, 16, buf);
        return header;
    }

    private static int sigBodyStart(String signature) {
        int i = 0;
        while (i < signature.length() && Character.isDigit(signature.charAt(i))) i++;
        return i;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}
