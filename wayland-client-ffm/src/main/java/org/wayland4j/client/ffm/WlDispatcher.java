package org.wayland4j.client.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

/**
 * Java callback for {@code wl_dispatcher_func_t}, the unified entry point
 * registered with {@code wl_proxy_add_dispatcher}.
 *
 * <p>Compared to the per-event {@code listener} array used by
 * {@code wl_proxy_add_listener}, the dispatcher API is much easier to
 * bridge across the FFM boundary because libwayland decodes the wire
 * arguments to a {@code union wl_argument[]} <em>before</em> calling us.
 * Generated per-interface listeners can be implemented on top of this
 * single entry point.
 *
 * <p>If the implementation throws, the exception is reported on
 * {@code System.err} and {@code -1} is returned to libwayland, which will
 * mark the connection as errored.
 */
@FunctionalInterface
public interface WlDispatcher {

    /**
     * Handle one incoming event.
     *
     * @param target  the proxy that received the event
     * @param opcode  the event opcode within {@code target.interface().events}
     * @param message the {@code wl_message} for that event (matches {@code opcode})
     * @param args    decoded arguments, already unwrapped from the
     *                {@code union wl_argument[]}
     */
    void dispatch(WlProxy target, int opcode, WlMessage message, List<WlArgument> args);

    /** Native function descriptor for {@code wl_dispatcher_func_t}. */
    FunctionDescriptor NATIVE_DESCRIPTOR = FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,   // user_data
            ValueLayout.ADDRESS,   // target proxy
            ValueLayout.JAVA_INT,  // opcode
            ValueLayout.ADDRESS,   // wl_message *
            ValueLayout.ADDRESS    // wl_argument *
    );

    /**
     * Bind {@code dispatcher} to a native upcall stub allocated in
     * {@code arena}. The stub stays valid for the lifetime of {@code arena}.
     *
     * <p>This is normally called from {@link WlProxy#addDispatcher} and
     * does not need to be invoked directly.
     */
    static MemorySegment toUpcallStub(WlDispatcher dispatcher, Arena arena) {
        try {
            MethodHandle handle = MethodHandles.lookup().findStatic(
                    WlDispatcher.class,
                    "dispatchUpcall",
                    MethodType.methodType(
                            int.class,
                            WlDispatcher.class,
                            MemorySegment.class,
                            MemorySegment.class,
                            int.class,
                            MemorySegment.class,
                            MemorySegment.class));
            MethodHandle bound = handle.bindTo(dispatcher);
            return WaylandNative.LINKER.upcallStub(bound, NATIVE_DESCRIPTOR, arena);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new WaylandClientException("Failed to bind WlDispatcher upcall", e);
        }
    }

    /**
     * Trampoline invoked by the upcall stub. Public only because
     * {@link MethodHandles.Lookup#findStatic} requires it; do not call
     * directly.
     */
    static int dispatchUpcall(
            WlDispatcher dispatcher,
            MemorySegment userData,
            MemorySegment target,
            int opcode,
            MemorySegment messagePtr,
            MemorySegment argsPtr) {
        try {
            WlMessage message = WlMessage.wrap(messagePtr);
            List<WlArgument> args = WlArgument.read(argsPtr, message.arguments());
            WlProxy proxy = WlProxy.wrap(target);
            dispatcher.dispatch(proxy, opcode, message, args);
            return 0;
        } catch (Throwable t) {
            // We must not let exceptions cross back into C — that's UB.
            t.printStackTrace(System.err);
            return -1;
        }
    }
}
