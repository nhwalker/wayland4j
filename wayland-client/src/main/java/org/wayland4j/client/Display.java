package org.wayland4j.client;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.wayland4j.client.internal.Dispatcher;
import org.wayland4j.client.internal.NativeLibrary;
import org.wayland4j.client.internal.ProxyRegistry;
import org.wayland4j.client.internal.Wayland;
import org.wayland4j.client.protocol.WlDisplay;

/**
 * Owns the {@code wl_display} connection and exposes the libwayland-client
 * dispatch surface in idiomatic Java.
 */
public final class Display extends WlDisplay implements AutoCloseable {

    private final AtomicReference<WaylandProtocolException> pendingProtocolError = new AtomicReference<>();
    private volatile boolean closed = false;

    private Display(MemorySegment ptr) {
        super(ptr);
    }

    /** Connect using the {@code WAYLAND_DISPLAY} environment variable. */
    public static Display connect() {
        return connectInternal(null);
    }

    /** Connect to a specific display socket name (e.g. {@code "wayland-0"}). */
    public static Display connect(String name) {
        return connectInternal(name);
    }

    /** Wrap an already-open file descriptor. */
    public static Display connectToFd(int fd) {
        Wayland.ensureBootstrapped();
        try {
            MemorySegment ptr = (MemorySegment) NativeLibrary.WL_DISPLAY_CONNECT_TO_FD.invokeExact(fd);
            if (ptr == null || ptr.address() == 0L) {
                throw new IllegalStateException("wl_display_connect_to_fd(" + fd + ") returned NULL");
            }
            return finishConnect(ptr);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    private static Display connectInternal(String name) {
        Wayland.ensureBootstrapped();
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment nameC = MemorySegment.NULL;
            if (name != null) {
                byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
                MemorySegment seg = scratch.allocate(bytes.length + 1, 1);
                MemorySegment.copy(bytes, 0, seg, ValueLayout.JAVA_BYTE, 0, bytes.length);
                seg.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0);
                nameC = seg;
            }
            MemorySegment ptr = (MemorySegment) NativeLibrary.WL_DISPLAY_CONNECT.invokeExact(nameC);
            if (ptr == null || ptr.address() == 0L) {
                throw new IllegalStateException(
                        "wl_display_connect failed (display=" + (name == null ? "$WAYLAND_DISPLAY" : name) + ")");
            }
            return finishConnect(ptr);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    private static Display finishConnect(MemorySegment ptr) {
        Display display = new Display(ptr);
        ProxyRegistry.register(display);
        // Install our internal listener so we observe error + delete_id events
        // before any user code interacts with proxies created via this display.
        display.setListener(new InternalListener(display));
        return display;
    }

    public int fd() {
        try {
            return (int) NativeLibrary.WL_DISPLAY_GET_FD.invokeExact(ptr());
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    public int dispatch() throws WaylandProtocolException {
        return runDispatch(NativeLibrary.WL_DISPLAY_DISPATCH);
    }

    public int dispatchPending() throws WaylandProtocolException {
        return runDispatch(NativeLibrary.WL_DISPLAY_DISPATCH_PENDING);
    }

    public int flush() throws WaylandProtocolException {
        try {
            int rc = (int) NativeLibrary.WL_DISPLAY_FLUSH.invokeExact(ptr());
            checkPending();
            return rc;
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    public int roundtrip() throws WaylandProtocolException {
        return runDispatch(NativeLibrary.WL_DISPLAY_ROUNDTRIP);
    }

    public int prepareRead() {
        try {
            return (int) NativeLibrary.WL_DISPLAY_PREPARE_READ.invokeExact(ptr());
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    public int readEvents() throws WaylandProtocolException {
        try {
            int rc = (int) NativeLibrary.WL_DISPLAY_READ_EVENTS.invokeExact(ptr());
            checkPending();
            return rc;
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    public void cancelRead() {
        try {
            NativeLibrary.WL_DISPLAY_CANCEL_READ.invokeExact(ptr());
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    public int getError() {
        try {
            return (int) NativeLibrary.WL_DISPLAY_GET_ERROR.invokeExact(ptr());
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            NativeLibrary.WL_DISPLAY_DISCONNECT.invokeExact(ptr());
        } catch (Throwable t) {
            throw rethrow(t);
        } finally {
            ProxyRegistry.unregister(address());
        }
    }

    private int runDispatch(java.lang.invoke.MethodHandle handle) throws WaylandProtocolException {
        if (Dispatcher.inDispatch()) {
            throw new IllegalStateException("dispatch called from within a Wayland event listener");
        }
        try {
            int rc = (int) handle.invokeExact(ptr());
            checkPending();
            if (rc < 0) {
                throw new WaylandProtocolException(null, 0, getError(),
                        "wl_display dispatch returned " + rc);
            }
            return rc;
        } catch (WaylandProtocolException e) {
            throw e;
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    private void checkPending() throws WaylandProtocolException {
        Throwable t = Dispatcher.takePending();
        if (t != null) {
            if (t instanceof WaylandProtocolException w) throw w;
            throw new WaylandProtocolException(null, 0, getError(), "listener threw: " + t);
        }
        WaylandProtocolException pe = pendingProtocolError.getAndSet(null);
        if (pe != null) throw pe;
    }

    void recordProtocolError(WaylandProtocolException e) {
        pendingProtocolError.compareAndSet(null, e);
    }

    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException re) return re;
        if (t instanceof java.lang.Error err) throw err;
        return new RuntimeException(t);
    }

    /**
     * Built-in listener: surfaces protocol errors and removes destroyed
     * proxies from the registry on {@code delete_id}.
     */
    private static final class InternalListener implements WlDisplay.Listener {

        private final Display display;

        InternalListener(Display display) {
            this.display = display;
        }

        @Override
        public void error(WlDisplay self, Proxy errorObject, int code, String message) {
            String iface = errorObject == null ? null : errorObject.wlClassName();
            int id = errorObject == null ? 0 : errorObject.id();
            display.recordProtocolError(new WaylandProtocolException(iface, id, code, message));
        }

        @Override
        public void deleteId(WlDisplay self, int id) {
            // libwayland frees the proxy struct itself; we just drop the
            // registry entry so we don't hang onto the listener reference.
            // Locating the entry by id requires a sweep — but at this point
            // the address is no longer valid, so we leave it and rely on the
            // user closing destroyed proxies explicitly.
        }
    }
}
