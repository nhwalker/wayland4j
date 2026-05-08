package org.wayland4j.client.internal;

import java.lang.foreign.MemorySegment;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.wayland4j.client.Proxy;

/**
 * Central registry that maps generated proxy classes to their protocol metadata.
 * Populated from {@code WaylandProtocols.<clinit>} (generated).
 */
public final class Wayland {

    public record ClassInfo(
            Class<? extends Proxy> klass,
            String wlName,
            int interfaceVersion,
            MemorySegment ifaceSegment,
            DispatchTable dispatchTable,
            Function<MemorySegment, ? extends Proxy> constructor
    ) {
    }

    private static final Map<Class<? extends Proxy>, ClassInfo> BY_CLASS = new ConcurrentHashMap<>();
    private static final Map<String, ClassInfo> BY_NAME = new ConcurrentHashMap<>();
    private static volatile boolean bootstrapped = false;

    private Wayland() {
    }

    /**
     * Trigger {@code WaylandProtocols.<clinit>} so the registry is populated. The
     * generated protocols class is in the same package as the proxies.
     */
    public static void ensureBootstrapped() {
        if (bootstrapped) return;
        synchronized (Wayland.class) {
            if (bootstrapped) return;
            try {
                Class.forName("org.wayland4j.client.protocol.WaylandProtocols", true, Wayland.class.getClassLoader());
                bootstrapped = true;
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "wayland4j: generated WaylandProtocols class not found; was the annotation processor run?", e);
            }
        }
    }

    public static <P extends Proxy> void register(
            Class<P> klass,
            String wlName,
            int interfaceVersion,
            MemorySegment ifaceSegment,
            DispatchTable dispatchTable,
            Function<MemorySegment, P> constructor) {
        ClassInfo info = new ClassInfo(klass, wlName, interfaceVersion, ifaceSegment, dispatchTable, constructor);
        BY_CLASS.put(klass, info);
        BY_NAME.put(wlName, info);
    }

    public static ClassInfo infoFor(Class<? extends Proxy> klass) {
        ensureBootstrapped();
        ClassInfo info = BY_CLASS.get(klass);
        if (info == null) {
            throw new IllegalStateException("no protocol info for " + klass.getName());
        }
        return info;
    }

    public static ClassInfo infoForOrNull(Class<? extends Proxy> klass) {
        ensureBootstrapped();
        return BY_CLASS.get(klass);
    }

    public static ClassInfo infoForName(String wlName) {
        ensureBootstrapped();
        ClassInfo info = BY_NAME.get(wlName);
        if (info == null) {
            throw new IllegalStateException("no protocol info for " + wlName);
        }
        return info;
    }

    public static Map<String, ClassInfo> snapshotByName() {
        ensureBootstrapped();
        return new HashMap<>(BY_NAME);
    }
}
