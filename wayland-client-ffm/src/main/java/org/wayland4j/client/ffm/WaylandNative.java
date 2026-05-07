package org.wayland4j.client.ffm;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Optional;

/**
 * Internal helper that owns the {@link Linker}, the
 * {@code libwayland-client} {@link SymbolLookup}, and a long-lived
 * {@link Arena} used for upcall stubs and other process-lifetime allocations.
 *
 * <p>Loading {@code libwayland-client.so} is deferred to the first use of
 * {@link #downcall} / {@link #downcallIfPresent} / {@link #findSymbol}, so
 * type-only consumers (struct layouts, signature parsing, fixed-point math)
 * can be loaded on systems without the native library installed.
 */
final class WaylandNative {

    private WaylandNative() {}

    static final Linker LINKER = Linker.nativeLinker();

    /**
     * Process-lifetime arena. Used for things that should stay valid as
     * long as the JVM is running, e.g. upcall stubs registered with
     * {@code wl_proxy_add_dispatcher}.
     */
    static final Arena GLOBAL_ARENA = Arena.ofShared();

    /** Canonical {@code size_t} layout for the current platform. */
    static final MemoryLayout SIZE_T = LINKER.canonicalLayouts().get("size_t");

    /** Canonical {@code int} layout (mirrors {@code int32_t} on every supported platform). */
    static final ValueLayout.OfInt C_INT = ValueLayout.JAVA_INT;

    static final AddressLayout C_POINTER = ValueLayout.ADDRESS;

    /**
     * Holder for {@link SymbolLookup} so libwayland-client is only opened
     * the first time a caller actually needs a native symbol.
     */
    private static final class ClientLibrary {
        static final SymbolLookup INSTANCE = open();

        private static SymbolLookup open() {
            // Try common library names in order. .so.0 SONAME ships in
            // libwayland-client0 (Debian/Ubuntu) and libwayland
            // (Fedora/Arch); the unversioned name only exists when the
            // -dev headers are installed.
            List<String> candidates = List.of(
                    "wayland-client",
                    "libwayland-client.so.0",
                    "libwayland-client.so");
            Throwable last = null;
            for (String name : candidates) {
                try {
                    return SymbolLookup.libraryLookup(name, GLOBAL_ARENA);
                } catch (RuntimeException e) {
                    last = e;
                }
            }
            UnsatisfiedLinkError err = new UnsatisfiedLinkError(
                    "Could not load libwayland-client (tried " + candidates + ")");
            if (last != null) {
                err.initCause(last);
            }
            throw err;
        }
    }

    /**
     * Resolve a downcall handle for a function in {@code libwayland-client}.
     *
     * @throws UnsatisfiedLinkError if the symbol does not exist in the loaded library
     */
    static MethodHandle downcall(String symbol, FunctionDescriptor descriptor) {
        MemorySegment addr = ClientLibrary.INSTANCE.find(symbol)
                .orElseThrow(() -> new UnsatisfiedLinkError(
                        "Symbol not found in libwayland-client: " + symbol));
        return LINKER.downcallHandle(addr, descriptor);
    }

    /**
     * Like {@link #downcall} but yields {@link Optional#empty()} if the
     * symbol is missing. Useful for entry points that were added in newer
     * libwayland versions.
     */
    static Optional<MethodHandle> downcallIfPresent(String symbol, FunctionDescriptor descriptor) {
        return ClientLibrary.INSTANCE.find(symbol)
                .map(addr -> LINKER.downcallHandle(addr, descriptor));
    }

    /** Look up a global data symbol, or {@link Optional#empty()} if absent. */
    static Optional<MemorySegment> findSymbol(String name) {
        return ClientLibrary.INSTANCE.find(name);
    }

    /** Read a NUL-terminated UTF-8 string from a possibly-zero pointer. */
    static String readCString(MemorySegment ptr) {
        if (ptr == null || ptr.address() == 0L) {
            return null;
        }
        MemorySegment unbounded = ptr.reinterpret(Long.MAX_VALUE);
        return unbounded.getString(0L);
    }

    /**
     * Eagerly attempt to load {@code libwayland-client}. Useful from tests
     * that want to skip when the library is unavailable rather than fail
     * during a later, less obvious downcall.
     *
     * @return true if loading succeeded
     */
    static boolean tryLoad() {
        try {
            ClientLibrary.INSTANCE.toString();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
