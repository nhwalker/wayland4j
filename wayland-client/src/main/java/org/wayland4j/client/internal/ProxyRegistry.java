package org.wayland4j.client.internal;

import java.util.concurrent.ConcurrentHashMap;
import org.wayland4j.client.Proxy;

/**
 * Maps native {@code wl_proxy*} addresses to the Java {@link Proxy} instances
 * that wrap them. Used to thread typed Java proxies through the dispatcher
 * upcall when libwayland delivers events with {@code object} or {@code new_id}
 * arguments.
 */
public final class ProxyRegistry {

    public static final class Entry {
        final Proxy proxy;
        volatile Object listener;

        Entry(Proxy proxy) {
            this.proxy = proxy;
        }

        public Proxy proxy() { return proxy; }
        public Object listener() { return listener; }
    }

    private static final ConcurrentHashMap<Long, Entry> BY_ADDRESS = new ConcurrentHashMap<>();

    private ProxyRegistry() {
    }

    public static Entry register(Proxy proxy) {
        return BY_ADDRESS.computeIfAbsent(proxy.address(), a -> new Entry(proxy));
    }

    public static Entry lookup(long address) {
        return BY_ADDRESS.get(address);
    }

    public static Proxy proxyAt(long address) {
        Entry e = BY_ADDRESS.get(address);
        return e == null ? null : e.proxy;
    }

    public static void unregister(long address) {
        BY_ADDRESS.remove(address);
    }

    public static void setListener(Proxy proxy, Object listener) {
        Entry e = register(proxy);
        e.listener = listener;
    }
}
