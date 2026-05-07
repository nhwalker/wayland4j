package org.wayland4j.client.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Optional;

/**
 * Wrapper for {@code struct wl_display *}, the root proxy returned by
 * {@code wl_display_connect}. Owns the connection to the compositor and
 * the default event queue.
 */
public final class WlDisplay extends WlProxy implements AutoCloseable {

    private static final MethodHandle WL_DISPLAY_CONNECT = WaylandNative.downcall(
            "wl_display_connect",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle WL_DISPLAY_CONNECT_TO_FD = WaylandNative.downcall(
            "wl_display_connect_to_fd",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    private static final MethodHandle WL_DISPLAY_DISCONNECT = WaylandNative.downcall(
            "wl_display_disconnect",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    private static final MethodHandle WL_DISPLAY_GET_FD = WaylandNative.downcall(
            "wl_display_get_fd",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    private static final MethodHandle WL_DISPLAY_DISPATCH = WaylandNative.downcall(
            "wl_display_dispatch",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    private static final MethodHandle WL_DISPLAY_DISPATCH_PENDING = WaylandNative.downcall(
            "wl_display_dispatch_pending",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    private static final MethodHandle WL_DISPLAY_DISPATCH_QUEUE = WaylandNative.downcall(
            "wl_display_dispatch_queue",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle WL_DISPLAY_DISPATCH_QUEUE_PENDING = WaylandNative.downcall(
            "wl_display_dispatch_queue_pending",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle WL_DISPLAY_ROUNDTRIP = WaylandNative.downcall(
            "wl_display_roundtrip",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    private static final MethodHandle WL_DISPLAY_ROUNDTRIP_QUEUE = WaylandNative.downcall(
            "wl_display_roundtrip_queue",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle WL_DISPLAY_FLUSH = WaylandNative.downcall(
            "wl_display_flush",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    private static final MethodHandle WL_DISPLAY_GET_ERROR = WaylandNative.downcall(
            "wl_display_get_error",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    private static final MethodHandle WL_DISPLAY_GET_PROTOCOL_ERROR = WaylandNative.downcall(
            "wl_display_get_protocol_error",
            FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,    // display
                    ValueLayout.ADDRESS,    // const wl_interface **interface (out)
                    ValueLayout.ADDRESS));  // uint32_t *id (out)

    private static final MethodHandle WL_DISPLAY_CREATE_QUEUE = WaylandNative.downcall(
            "wl_display_create_queue",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle WL_DISPLAY_PREPARE_READ = WaylandNative.downcall(
            "wl_display_prepare_read",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    private static final MethodHandle WL_DISPLAY_PREPARE_READ_QUEUE = WaylandNative.downcall(
            "wl_display_prepare_read_queue",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle WL_DISPLAY_READ_EVENTS = WaylandNative.downcall(
            "wl_display_read_events",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    private static final MethodHandle WL_DISPLAY_CANCEL_READ = WaylandNative.downcall(
            "wl_display_cancel_read",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    private boolean disconnected;

    private WlDisplay(MemorySegment handle) {
        super(handle);
    }

    /**
     * {@code wl_display_connect(NULL)} — connect to {@code $WAYLAND_DISPLAY}.
     *
     * @return the display, or {@link Optional#empty()} if the connection failed
     */
    public static Optional<WlDisplay> connect() {
        return connect(null);
    }

    /**
     * {@code wl_display_connect(name)} — connect to a named compositor
     * socket. {@code null} is equivalent to {@link #connect()}.
     */
    public static Optional<WlDisplay> connect(String name) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment cName = name == null ? MemorySegment.NULL : scratch.allocateFrom(name);
            MemorySegment ptr = (MemorySegment) WL_DISPLAY_CONNECT.invokeExact(cName);
            if (ptr.address() == 0L) {
                return Optional.empty();
            }
            return Optional.of(new WlDisplay(ptr));
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_connect failed", t);
        }
    }

    /** {@code wl_display_connect_to_fd}. Takes ownership of {@code fd}. */
    public static Optional<WlDisplay> connectToFd(int fd) {
        try {
            MemorySegment ptr = (MemorySegment) WL_DISPLAY_CONNECT_TO_FD.invokeExact(fd);
            if (ptr.address() == 0L) {
                return Optional.empty();
            }
            return Optional.of(new WlDisplay(ptr));
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_connect_to_fd failed", t);
        }
    }

    /** {@code wl_display_get_fd}. */
    public int fd() {
        try {
            return (int) WL_DISPLAY_GET_FD.invokeExact(handle);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_get_fd failed", t);
        }
    }

    /**
     * {@code wl_display_dispatch} — block until at least one event is
     * dispatched on the default queue. Returns the number of events
     * dispatched, or -1 on error.
     */
    public int dispatch() {
        try {
            return (int) WL_DISPLAY_DISPATCH.invokeExact(handle);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_dispatch failed", t);
        }
    }

    /** {@code wl_display_dispatch_pending} — non-blocking variant. */
    public int dispatchPending() {
        try {
            return (int) WL_DISPLAY_DISPATCH_PENDING.invokeExact(handle);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_dispatch_pending failed", t);
        }
    }

    /** {@code wl_display_dispatch_queue}. */
    public int dispatch(WlEventQueue queue) {
        try {
            return (int) WL_DISPLAY_DISPATCH_QUEUE.invokeExact(handle, queue.address());
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_dispatch_queue failed", t);
        }
    }

    /** {@code wl_display_dispatch_queue_pending}. */
    public int dispatchPending(WlEventQueue queue) {
        try {
            return (int) WL_DISPLAY_DISPATCH_QUEUE_PENDING.invokeExact(handle, queue.address());
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_dispatch_queue_pending failed", t);
        }
    }

    /**
     * {@code wl_display_roundtrip} — block until the server has processed
     * all queued requests on the default queue.
     */
    public int roundtrip() {
        try {
            return (int) WL_DISPLAY_ROUNDTRIP.invokeExact(handle);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_roundtrip failed", t);
        }
    }

    /** {@code wl_display_roundtrip_queue}. */
    public int roundtrip(WlEventQueue queue) {
        try {
            return (int) WL_DISPLAY_ROUNDTRIP_QUEUE.invokeExact(handle, queue.address());
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_roundtrip_queue failed", t);
        }
    }

    /**
     * {@code wl_display_flush} — push any buffered outgoing requests to
     * the socket. Returns the number of bytes sent, or -1 on error.
     */
    public int flush() {
        try {
            return (int) WL_DISPLAY_FLUSH.invokeExact(handle);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_flush failed", t);
        }
    }

    /**
     * {@code wl_display_get_error} — last fatal error on the connection,
     * or 0 if none. The value is an {@code errno} code.
     */
    public int lastError() {
        try {
            return (int) WL_DISPLAY_GET_ERROR.invokeExact(handle);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_get_error failed", t);
        }
    }

    /**
     * Result of {@code wl_display_get_protocol_error}.
     *
     * @param code      the protocol-specific error code
     * @param interfaceName name of the interface that raised the error,
     *                  or null if there is no current protocol error
     * @param id        object id of the offending proxy, or 0 when
     *                  {@code interfaceName} is null
     */
    public record ProtocolError(int code, String interfaceName, int id) {}

    /** {@code wl_display_get_protocol_error}. */
    public ProtocolError protocolError() {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment ifaceOut = scratch.allocate(ValueLayout.ADDRESS);
            MemorySegment idOut = scratch.allocate(ValueLayout.JAVA_INT);
            int code = (int) WL_DISPLAY_GET_PROTOCOL_ERROR.invokeExact(handle, ifaceOut, idOut);
            MemorySegment ifacePtr = ifaceOut.get(ValueLayout.ADDRESS, 0L);
            String name = ifacePtr.address() == 0L
                    ? null
                    : WlInterface.wrap(ifacePtr).name();
            int id = idOut.get(ValueLayout.JAVA_INT, 0L);
            return new ProtocolError(code, name, id);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_get_protocol_error failed", t);
        }
    }

    /** {@code wl_display_create_queue}. */
    public WlEventQueue createQueue() {
        try {
            MemorySegment ptr = (MemorySegment) WL_DISPLAY_CREATE_QUEUE.invokeExact(handle);
            if (ptr.address() == 0L) {
                throw new WaylandClientException("wl_display_create_queue returned NULL");
            }
            return new WlEventQueue(ptr);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_create_queue failed", t);
        }
    }

    /** {@code wl_display_prepare_read}. */
    public int prepareRead() {
        try {
            return (int) WL_DISPLAY_PREPARE_READ.invokeExact(handle);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_prepare_read failed", t);
        }
    }

    /** {@code wl_display_prepare_read_queue}. */
    public int prepareRead(WlEventQueue queue) {
        try {
            return (int) WL_DISPLAY_PREPARE_READ_QUEUE.invokeExact(handle, queue.address());
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_prepare_read_queue failed", t);
        }
    }

    /** {@code wl_display_read_events}. */
    public int readEvents() {
        try {
            return (int) WL_DISPLAY_READ_EVENTS.invokeExact(handle);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_read_events failed", t);
        }
    }

    /** {@code wl_display_cancel_read}. */
    public void cancelRead() {
        try {
            WL_DISPLAY_CANCEL_READ.invokeExact(handle);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_cancel_read failed", t);
        }
    }

    /** {@code wl_display_disconnect}. Idempotent. */
    @Override
    public void destroy() {
        if (disconnected) {
            return;
        }
        disconnected = true;
        try {
            WL_DISPLAY_DISCONNECT.invokeExact(handle);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_display_disconnect failed", t);
        }
    }

    /** Alias for {@link #destroy()}. */
    @Override
    public void close() {
        destroy();
    }
}
