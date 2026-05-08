package org.wayland4j.client.internal;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.ConcurrentHashMap;
import org.wayland4j.client.Proxy;

/**
 * Maps native {@code wl_proxy*} addresses to the Java {@link Proxy} instances
 * that wrap them. Used to thread typed Java proxies through the dispatcher
 * upcall when libwayland delivers events with {@code object} or {@code new_id}
 * arguments.
 *
 * <p>A second index keyed by {@code (display address, id)} supports
 * {@code wl_display.delete_id} cleanup: when the server tells us a proxy id is
 * gone, libwayland frees the {@code wl_proxy} struct and the address becomes
 * invalid, so we have to find the entry by id instead.
 *
 * <p>{@code wl_proxy_get_display} is only present in libwayland ≥ 1.23. On
 * older libraries we fall back to a single id space — that means proxies from
 * multiple simultaneous {@link org.wayland4j.client.Display} connections share
 * one id table, which can cause stale entries to be dropped if both displays
 * happen to assign the same id. Production code that needs robust multi-display
 * support should run against libwayland 1.23+.
 */
public final class ProxyRegistry {

    public static final class Entry {
        final Proxy proxy;
        final long displayAddress;
        final int id;
        volatile Object listener;

        Entry(Proxy proxy, long displayAddress, int id) {
            this.proxy = proxy;
            this.displayAddress = displayAddress;
            this.id = id;
        }

        public Proxy proxy() { return proxy; }
        public Object listener() { return listener; }
    }

    private static final ConcurrentHashMap<Long, Entry> BY_ADDRESS = new ConcurrentHashMap<>();
    // displayAddr -> (id -> proxy address). When wl_proxy_get_display isn't
    // available we use the sentinel key 0 for the flat-fallback table.
    private static final ConcurrentHashMap<Long, ConcurrentHashMap<Integer, Long>> ID_INDEX = new ConcurrentHashMap<>();

    private ProxyRegistry() {
    }

    public static Entry register(Proxy proxy) {
        long addr = proxy.address();
        Entry existing = BY_ADDRESS.get(addr);
        if (existing != null) return existing;
        long displayAddr = displayAddressOf(proxy.ptr());
        int id = idOf(proxy.ptr());
        Entry created = new Entry(proxy, displayAddr, id);
        Entry prior = BY_ADDRESS.putIfAbsent(addr, created);
        if (prior != null) return prior;
        ID_INDEX.computeIfAbsent(displayAddr, k -> new ConcurrentHashMap<>()).put(id, addr);
        return created;
    }

    public static Entry lookup(long address) {
        return BY_ADDRESS.get(address);
    }

    public static Proxy proxyAt(long address) {
        Entry e = BY_ADDRESS.get(address);
        return e == null ? null : e.proxy;
    }

    public static void unregister(long address) {
        Entry e = BY_ADDRESS.remove(address);
        if (e == null) return;
        ConcurrentHashMap<Integer, Long> perDisplay = ID_INDEX.get(e.displayAddress);
        if (perDisplay != null) {
            perDisplay.remove(e.id, address);
        }
    }

    /**
     * Drop the registry entry for a proxy that the server already deleted —
     * looked up by {@code (displayAddr, id)} because the native address is no
     * longer valid by the time {@code wl_display.delete_id} arrives. When the
     * id index runs in flat-fallback mode (libwayland &lt; 1.23) the
     * {@code displayAddress} argument is ignored.
     */
    public static void unregisterById(long displayAddress, int id) {
        long key = NativeLibrary.WL_PROXY_GET_DISPLAY != null ? displayAddress : 0L;
        ConcurrentHashMap<Integer, Long> perDisplay = ID_INDEX.get(key);
        if (perDisplay == null) return;
        Long addr = perDisplay.remove(id);
        if (addr != null) {
            BY_ADDRESS.remove(addr);
        }
    }

    public static void setListener(Proxy proxy, Object listener) {
        Entry e = register(proxy);
        e.listener = listener;
    }

    private static long displayAddressOf(MemorySegment proxyPtr) {
        if (NativeLibrary.WL_PROXY_GET_DISPLAY == null) {
            return 0L; // flat fallback
        }
        try {
            MemorySegment d = (MemorySegment) NativeLibrary.WL_PROXY_GET_DISPLAY.invokeExact(proxyPtr);
            return d == null ? 0L : d.address();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static int idOf(MemorySegment proxyPtr) {
        try {
            return (int) NativeLibrary.WL_PROXY_GET_ID.invokeExact(proxyPtr);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
