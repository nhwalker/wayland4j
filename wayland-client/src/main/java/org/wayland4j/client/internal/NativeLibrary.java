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
    public static final MethodHandle WL_PROXY_ADD_DISPATCHER = downcall(
            "wl_proxy_add_dispatcher", FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,    // proxy
                    ValueLayout.ADDRESS,    // wl_dispatcher_func_t
                    ValueLayout.ADDRESS,    // dispatcher_data
                    ValueLayout.ADDRESS     // user data
            ));

    public static final int WL_MARSHAL_FLAG_DESTROY = 1;

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
