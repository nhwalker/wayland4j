package org.wayland4j.client.internal;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the {@code struct wl_interface} / {@code struct wl_message} graph that
 * libwayland-client requires. Cyclic references between interfaces are
 * resolved with a two-pass initialization in a single shared arena that lives
 * for the JVM lifetime.
 *
 * <p>{@link #populate(Descriptor[])} is additive: each generated
 * {@code WaylandProtocols} class registers its own descriptors, and later
 * calls can reference interfaces built by earlier ones (e.g. an extension
 * referring to {@code wl_surface}). Names that have already been built are
 * skipped, so loading the same protocol twice is a no-op.
 */
public final class WlInterfaceArena {

    public static final MemoryLayout WL_MESSAGE = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("name"),
            ValueLayout.ADDRESS.withName("signature"),
            ValueLayout.ADDRESS.withName("types")
    ).withName("wl_message");

    public static final MemoryLayout WL_INTERFACE = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("name"),
            ValueLayout.JAVA_INT.withName("version"),
            ValueLayout.JAVA_INT.withName("method_count"),
            ValueLayout.ADDRESS.withName("methods"),
            ValueLayout.JAVA_INT.withName("event_count"),
            MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS.withName("events")
    ).withName("wl_interface");

    private static final long WL_MESSAGE_SIZE = WL_MESSAGE.byteSize();          // 24
    private static final long WL_INTERFACE_SIZE = WL_INTERFACE.byteSize();      // 40
    private static final long IFACE_OFF_NAME = 0L;
    private static final long IFACE_OFF_VERSION = 8L;
    private static final long IFACE_OFF_METHOD_COUNT = 12L;
    private static final long IFACE_OFF_METHODS = 16L;
    private static final long IFACE_OFF_EVENT_COUNT = 24L;
    private static final long IFACE_OFF_EVENTS = 32L;
    private static final long MSG_OFF_NAME = 0L;
    private static final long MSG_OFF_SIGNATURE = 8L;
    private static final long MSG_OFF_TYPES = 16L;

    private static final Map<String, MemorySegment> INTERFACES = new LinkedHashMap<>();

    private WlInterfaceArena() {
    }

    public record Descriptor(String name, int version, Message[] requests, Message[] events) {
    }

    /**
     * @param types one entry per slot in {@code signature}; either an interface
     *              name (resolved in pass 2) or {@code null} for primitive slots
     *              and dynamic-new-id slots.
     */
    public record Message(String name, String signature, String[] types) {
    }

    /**
     * Add the listed descriptors to the global wl_interface graph. Skips
     * descriptors whose names are already registered (so calling twice with
     * the same descriptor list is harmless). Returns a snapshot of every
     * registered name → segment after this call, so callers can pull out
     * pointers for whatever names they care about.
     */
    public static synchronized Map<String, MemorySegment> populate(Descriptor[] descriptors) {
        // Pass 1: allocate a wl_interface for every name we don't already have.
        Map<String, MemorySegment> newSegments = new HashMap<>();
        for (Descriptor d : descriptors) {
            if (INTERFACES.containsKey(d.name())) continue;
            MemorySegment seg = NativeLibrary.LIB_ARENA.allocate(WL_INTERFACE_SIZE, 8);
            seg.fill((byte) 0);
            newSegments.put(d.name(), seg);
        }

        // Combined view used to resolve cross-references in pass 2; covers
        // both previously-built interfaces and the ones we just allocated.
        Map<String, MemorySegment> resolution = new HashMap<>(INTERFACES);
        resolution.putAll(newSegments);

        // Pass 2: fill in only the freshly-allocated interfaces.
        for (Descriptor d : descriptors) {
            MemorySegment ifaceSeg = newSegments.get(d.name());
            if (ifaceSeg == null) continue; // skipped above
            MemorySegment nameStr = allocateUtf8(d.name());
            MemorySegment requestsSeg = buildMessageArray(d.requests(), resolution);
            MemorySegment eventsSeg = buildMessageArray(d.events(), resolution);

            ifaceSeg.set(ValueLayout.ADDRESS, IFACE_OFF_NAME, nameStr);
            ifaceSeg.set(ValueLayout.JAVA_INT, IFACE_OFF_VERSION, d.version());
            ifaceSeg.set(ValueLayout.JAVA_INT, IFACE_OFF_METHOD_COUNT, d.requests().length);
            ifaceSeg.set(ValueLayout.ADDRESS, IFACE_OFF_METHODS, requestsSeg);
            ifaceSeg.set(ValueLayout.JAVA_INT, IFACE_OFF_EVENT_COUNT, d.events().length);
            ifaceSeg.set(ValueLayout.ADDRESS, IFACE_OFF_EVENTS, eventsSeg);
        }

        INTERFACES.putAll(newSegments);
        return new HashMap<>(INTERFACES);
    }

    private static MemorySegment buildMessageArray(Message[] messages, Map<String, MemorySegment> ifaceSegments) {
        if (messages.length == 0) {
            return MemorySegment.NULL;
        }
        MemorySegment array = NativeLibrary.LIB_ARENA.allocate(WL_MESSAGE_SIZE * messages.length, 8);
        array.fill((byte) 0);
        for (int i = 0; i < messages.length; i++) {
            Message m = messages[i];
            long base = i * WL_MESSAGE_SIZE;
            MemorySegment nameStr = allocateUtf8(m.name());
            MemorySegment sigStr = allocateUtf8(m.signature());
            MemorySegment typesArr = buildTypesArray(m.types(), ifaceSegments);
            array.set(ValueLayout.ADDRESS, base + MSG_OFF_NAME, nameStr);
            array.set(ValueLayout.ADDRESS, base + MSG_OFF_SIGNATURE, sigStr);
            array.set(ValueLayout.ADDRESS, base + MSG_OFF_TYPES, typesArr);
        }
        return array;
    }

    private static MemorySegment buildTypesArray(String[] types, Map<String, MemorySegment> ifaceSegments) {
        if (types.length == 0) {
            return MemorySegment.NULL;
        }
        MemorySegment arr = NativeLibrary.LIB_ARENA.allocate((long) types.length * 8L, 8);
        arr.fill((byte) 0);
        for (int i = 0; i < types.length; i++) {
            String tName = types[i];
            if (tName == null) {
                arr.set(ValueLayout.ADDRESS, i * 8L, MemorySegment.NULL);
            } else {
                MemorySegment ifaceSeg = ifaceSegments.get(tName);
                if (ifaceSeg == null) {
                    throw new IllegalStateException("undefined wl_interface reference: " + tName
                            + " (load the protocol that defines it before referencing it)");
                }
                arr.set(ValueLayout.ADDRESS, i * 8L, ifaceSeg);
            }
        }
        return arr;
    }

    private static MemorySegment allocateUtf8(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        MemorySegment seg = NativeLibrary.LIB_ARENA.allocate(bytes.length + 1, 1);
        MemorySegment.copy(bytes, 0, seg, ValueLayout.JAVA_BYTE, 0, bytes.length);
        seg.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0);
        return seg;
    }

    public static synchronized MemorySegment get(String name) {
        MemorySegment seg = INTERFACES.get(name);
        if (seg == null) {
            throw new IllegalArgumentException("unknown wl_interface: " + name);
        }
        return seg;
    }
}
