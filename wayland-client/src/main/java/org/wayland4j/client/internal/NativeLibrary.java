package org.wayland4j.client.internal;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves {@code libwayland-client} and binds the {@link MethodHandle}s the
 * runtime needs.
 */
public final class NativeLibrary {

    public static final Arena LIB_ARENA = Arena.ofShared();
    public static final Linker LINKER = Linker.nativeLinker();
    public static final SymbolLookup LOOKUP = resolveLibrary();

    static {
        // wl_proxy_marshal_array_flags landed in libwayland 1.20.91 and is the marshal
        // entrypoint we always use. If it isn't present we'd fail later with a less
        // obvious "missing symbol" error; surface a clearer message up front.
        LOOKUP.find("wl_proxy_marshal_array_flags").orElseThrow(() -> new UnsatisfiedLinkError(
                "wayland4j requires libwayland-client ≥ 1.20.91 (symbol "
                        + "wl_proxy_marshal_array_flags not found in the resolved library). "
                        + "Set WAYLAND4J_CLIENT_LIB to override the loaded path."));
    }

    public static final MethodHandle WL_DISPLAY_CONNECT = downcall(
            "wl_display_connect", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    public static final MethodHandle WL_DISPLAY_CONNECT_TO_FD = downcall(
            "wl_display_connect_to_fd", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
    public static final MethodHandle WL_DISPLAY_DISCONNECT = downcall(
            "wl_display_disconnect", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
    public static final MethodHandle WL_DISPLAY_GET_FD = downcall(
            "wl_display_get_fd", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
    public static final MethodHandle WL_DISPLAY_DISPATCH = downcall(
            "wl_display_dispatch", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
    public static final MethodHandle WL_DISPLAY_DISPATCH_PENDING = downcall(
            "wl_display_dispatch_pending", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
    public static final MethodHandle WL_DISPLAY_FLUSH = downcall(
            "wl_display_flush", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
    public static final MethodHandle WL_DISPLAY_ROUNDTRIP = downcall(
            "wl_display_roundtrip", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
    public static final MethodHandle WL_DISPLAY_PREPARE_READ = downcall(
            "wl_display_prepare_read", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
    public static final MethodHandle WL_DISPLAY_READ_EVENTS = downcall(
            "wl_display_read_events", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
    public static final MethodHandle WL_DISPLAY_CANCEL_READ = downcall(
            "wl_display_cancel_read", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
    public static final MethodHandle WL_DISPLAY_GET_ERROR = downcall(
            "wl_display_get_error", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
    public static final MethodHandle WL_DISPLAY_GET_PROTOCOL_ERROR = downcall(
            "wl_display_get_protocol_error", FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, // wl_display
                    ValueLayout.ADDRESS, // out: const struct wl_interface **
                    ValueLayout.ADDRESS  // out: uint32_t *id
            ));

    public static final MethodHandle WL_PROXY_MARSHAL_ARRAY_FLAGS = downcall(
            "wl_proxy_marshal_array_flags", FunctionDescriptor.of(
                    ValueLayout.ADDRESS,    // returns wl_proxy*
                    ValueLayout.ADDRESS,    // proxy
                    ValueLayout.JAVA_INT,   // opcode
                    ValueLayout.ADDRESS,    // const struct wl_interface *interface
                    ValueLayout.JAVA_INT,   // version
                    ValueLayout.JAVA_INT,   // flags
                    ValueLayout.ADDRESS     // union wl_argument *args
            ));
    public static final MethodHandle WL_PROXY_DESTROY = downcall(
            "wl_proxy_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
    public static final MethodHandle WL_PROXY_GET_ID = downcall(
            "wl_proxy_get_id", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
    public static final MethodHandle WL_PROXY_GET_VERSION = downcall(
            "wl_proxy_get_version", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
    public static final MethodHandle WL_PROXY_GET_CLASS = downcall(
            "wl_proxy_get_class", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    /** Optional: only present in libwayland-client ≥ 1.23. {@code null} on older libraries. */
    public static final MethodHandle WL_PROXY_GET_DISPLAY = downcallOptional(
            "wl_proxy_get_display", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    public static final MethodHandle WL_PROXY_ADD_DISPATCHER = downcall(
            "wl_proxy_add_dispatcher", FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,    // proxy
                    ValueLayout.ADDRESS,    // wl_dispatcher_func_t
                    ValueLayout.ADDRESS,    // dispatcher_data
                    ValueLayout.ADDRESS     // user data
            ));
    public static final MethodHandle WL_PROXY_SET_QUEUE = downcall(
            "wl_proxy_set_queue",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    public static final MethodHandle WL_EVENT_QUEUE_CREATE_NAMED = downcallOptional(
            "wl_display_create_queue_with_name",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    public static final MethodHandle WL_EVENT_QUEUE_CREATE = downcall(
            "wl_display_create_queue",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    public static final MethodHandle WL_EVENT_QUEUE_DESTROY = downcall(
            "wl_event_queue_destroy",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
    public static final MethodHandle WL_DISPLAY_DISPATCH_QUEUE = downcall(
            "wl_display_dispatch_queue",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    public static final MethodHandle WL_DISPLAY_DISPATCH_QUEUE_PENDING = downcall(
            "wl_display_dispatch_queue_pending",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    public static final MethodHandle WL_DISPLAY_ROUNDTRIP_QUEUE = downcall(
            "wl_display_roundtrip_queue",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    public static final int WL_MARSHAL_FLAG_DESTROY = 1;

    static {
        // Route libwayland's client-side log handler to java.util.logging. Best-effort:
        // missing symbols (e.g. on a stripped libc) are silently ignored.
        LogBridge.install();
    }

    private NativeLibrary() {
    }

    private static SymbolLookup resolveLibrary() {
        List<String> tried = new ArrayList<>();
        // Order: SONAME, then unsuffixed name, then env override.
        String[] candidates = {
                "libwayland-client.so.0",
                "libwayland-client.so",
                System.getenv("WAYLAND4J_CLIENT_LIB")
        };
        for (String c : candidates) {
            if (c == null || c.isEmpty()) continue;
            tried.add(c);
            try {
                return SymbolLookup.libraryLookup(c, LIB_ARENA);
            } catch (RuntimeException | Error ignored) {
                // try next
            }
        }
        throw new UnsatisfiedLinkError(
                "wayland4j: could not load libwayland-client. Tried: " + tried
                        + ". Set WAYLAND4J_CLIENT_LIB to override.");
    }

    private static MethodHandle downcall(String symbol, FunctionDescriptor desc) {
        MemorySegment addr = LOOKUP.find(symbol)
                .orElseThrow(() -> new UnsatisfiedLinkError("missing symbol: " + symbol));
        return LINKER.downcallHandle(addr, desc);
    }

    /** {@code null} if the symbol isn't present in the loaded library. */
    private static MethodHandle downcallOptional(String symbol, FunctionDescriptor desc) {
        return LOOKUP.find(symbol)
                .map(addr -> LINKER.downcallHandle(addr, desc))
                .orElse(null);
    }

    public static MemorySegment proxyClassName(MemorySegment proxy) {
        try {
            MemorySegment ptr = (MemorySegment) WL_PROXY_GET_CLASS.invokeExact(proxy);
            if (ptr.address() == 0L) return null;
            return ptr.reinterpret(Long.MAX_VALUE);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
