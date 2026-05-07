package org.wayland4j.client.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Objects;

/**
 * Wrapper for {@code struct wl_proxy *}, the opaque object handle used by
 * libwayland.
 *
 * <p>Subclassed by {@link WlDisplay} for the special root proxy returned
 * by {@code wl_display_connect}. Per-interface wrappers (generated from
 * the protocol XML) typically extend this class as well.
 */
public sealed class WlProxy permits WlDisplay {

    private static final MethodHandle WL_PROXY_MARSHAL_ARRAY_FLAGS = WaylandNative.downcall(
            "wl_proxy_marshal_array_flags",
            FunctionDescriptor.of(
                    ValueLayout.ADDRESS,    // returns wl_proxy *
                    ValueLayout.ADDRESS,    // proxy
                    ValueLayout.JAVA_INT,   // opcode
                    ValueLayout.ADDRESS,    // wl_interface *
                    ValueLayout.JAVA_INT,   // version
                    ValueLayout.JAVA_INT,   // flags
                    ValueLayout.ADDRESS));  // wl_argument *

    private static final MethodHandle WL_PROXY_DESTROY = WaylandNative.downcall(
            "wl_proxy_destroy",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    private static final MethodHandle WL_PROXY_GET_ID = WaylandNative.downcall(
            "wl_proxy_get_id",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    private static final MethodHandle WL_PROXY_GET_VERSION = WaylandNative.downcall(
            "wl_proxy_get_version",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    private static final MethodHandle WL_PROXY_GET_CLASS = WaylandNative.downcall(
            "wl_proxy_get_class",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle WL_PROXY_SET_USER_DATA = WaylandNative.downcall(
            "wl_proxy_set_user_data",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle WL_PROXY_GET_USER_DATA = WaylandNative.downcall(
            "wl_proxy_get_user_data",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle WL_PROXY_SET_QUEUE = WaylandNative.downcall(
            "wl_proxy_set_queue",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle WL_PROXY_ADD_DISPATCHER = WaylandNative.downcall(
            "wl_proxy_add_dispatcher",
            FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,    // proxy
                    ValueLayout.ADDRESS,    // dispatcher_func
                    ValueLayout.ADDRESS,    // dispatcher_data
                    ValueLayout.ADDRESS));  // data

    final MemorySegment handle;

    WlProxy(MemorySegment handle) {
        this.handle = Objects.requireNonNull(handle);
    }

    /**
     * Wrap a raw {@code wl_proxy *}. The pointer is reinterpreted as an
     * unbounded address — libwayland controls the lifetime, not the JVM.
     */
    public static WlProxy wrap(MemorySegment ptr) {
        if (ptr == null || ptr.address() == 0L) {
            throw new WaylandClientException("Cannot wrap NULL wl_proxy pointer");
        }
        return new WlProxy(ptr);
    }

    /** The underlying {@code wl_proxy *} address. */
    public MemorySegment address() {
        return handle;
    }

    /** {@code wl_proxy_get_id} — the object id assigned by the server. */
    public int id() {
        try {
            return (int) WL_PROXY_GET_ID.invokeExact(handle);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_proxy_get_id failed", t);
        }
    }

    /** {@code wl_proxy_get_version} — the negotiated interface version. */
    public int version() {
        try {
            return (int) WL_PROXY_GET_VERSION.invokeExact(handle);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_proxy_get_version failed", t);
        }
    }

    /** {@code wl_proxy_get_class} — the interface name (e.g. {@code "wl_surface"}). */
    public String className() {
        try {
            MemorySegment ptr = (MemorySegment) WL_PROXY_GET_CLASS.invokeExact(handle);
            return WaylandNative.readCString(ptr);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_proxy_get_class failed", t);
        }
    }

    /** {@code wl_proxy_set_queue} — reassign the queue this proxy dispatches on. */
    public void setQueue(WlEventQueue queue) {
        MemorySegment q = queue == null ? MemorySegment.NULL : queue.address();
        try {
            WL_PROXY_SET_QUEUE.invokeExact(handle, q);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_proxy_set_queue failed", t);
        }
    }

    /** {@code wl_proxy_set_user_data}. */
    public void setUserData(MemorySegment data) {
        MemorySegment d = data == null ? MemorySegment.NULL : data;
        try {
            WL_PROXY_SET_USER_DATA.invokeExact(handle, d);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_proxy_set_user_data failed", t);
        }
    }

    /** {@code wl_proxy_get_user_data}. May return {@link MemorySegment#NULL}. */
    public MemorySegment getUserData() {
        try {
            return (MemorySegment) WL_PROXY_GET_USER_DATA.invokeExact(handle);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_proxy_get_user_data failed", t);
        }
    }

    /**
     * {@code wl_proxy_marshal_array_flags} — send a request, optionally
     * creating a new proxy for the first {@code new_id} argument.
     *
     * @param opcode      the request opcode within this proxy's interface
     * @param newInterface {@code wl_interface *} for the new object, or null
     *                     if the request does not create one
     * @param version     version of the new object (ignored when
     *                    {@code newInterface == null})
     * @param flags       bitmask of {@link WlMarshalFlags}
     * @param args        the request arguments
     * @param scratch     allocator for the temporary {@code wl_argument[]}
     *                    and string copies — typically {@code Arena.ofConfined()}
     *                    around the call
     * @return the new proxy, or null if the request does not create one
     */
    public WlProxy marshal(
            int opcode,
            WlInterface newInterface,
            int version,
            int flags,
            List<? extends WlArgument> args,
            SegmentAllocator scratch) {
        MemorySegment ifaceAddr = newInterface == null
                ? MemorySegment.NULL
                : newInterface.address();
        MemorySegment argv = WlArgument.write(scratch, args);
        try {
            MemorySegment ret = (MemorySegment) WL_PROXY_MARSHAL_ARRAY_FLAGS.invokeExact(
                    handle, opcode, ifaceAddr, version, flags, argv);
            if (ret.address() == 0L) {
                return null;
            }
            return new WlProxy(ret);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_proxy_marshal_array_flags failed", t);
        }
    }

    /**
     * Register a Java {@link WlDispatcher} on this proxy. The upcall stub
     * is allocated in {@link WaylandNative#GLOBAL_ARENA}, i.e. it stays
     * valid for the lifetime of the JVM.
     *
     * @return 0 on success, -1 if a dispatcher is already set
     */
    public int addDispatcher(WlDispatcher dispatcher) {
        return addDispatcher(dispatcher, WaylandNative.GLOBAL_ARENA);
    }

    /**
     * Register a Java {@link WlDispatcher} on this proxy, allocating the
     * upcall stub in {@code stubLifetime}. The arena must outlive the
     * proxy or libwayland will call into a freed stub.
     */
    public int addDispatcher(WlDispatcher dispatcher, Arena stubLifetime) {
        Objects.requireNonNull(dispatcher);
        MemorySegment stub = WlDispatcher.toUpcallStub(dispatcher, stubLifetime);
        try {
            return (int) WL_PROXY_ADD_DISPATCHER.invokeExact(
                    handle, stub, MemorySegment.NULL, MemorySegment.NULL);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_proxy_add_dispatcher failed", t);
        }
    }

    /**
     * {@code wl_proxy_destroy} — release this proxy.
     *
     * <p>Subclasses may override to redirect to a more specific destructor;
     * {@link WlDisplay#destroy()}, for example, calls
     * {@code wl_display_disconnect}.
     */
    public void destroy() {
        try {
            WL_PROXY_DESTROY.invokeExact(handle);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_proxy_destroy failed", t);
        }
    }
}
