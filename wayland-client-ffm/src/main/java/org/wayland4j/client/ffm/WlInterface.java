package org.wayland4j.client.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Wrapper for {@code struct wl_interface}:
 * <pre>{@code
 * struct wl_interface {
 *     const char *name;
 *     int version;
 *     int method_count;
 *     const struct wl_message *methods;
 *     int event_count;
 *     const struct wl_message *events;
 * };
 * }</pre>
 *
 * <p>libwayland exports one of these per protocol interface. The core
 * client interfaces ({@code wl_display_interface},
 * {@code wl_callback_interface}, {@code wl_registry_interface}, ...) are
 * exposed as global symbols by {@code libwayland-client}; use
 * {@link #lookup(String)} to resolve them by symbol name.
 */
public final class WlInterface {

    /**
     * Layout of {@code wl_interface}. The 4 bytes of padding between
     * {@code event_count} and {@code events} make the struct 40 bytes wide
     * on every 64-bit ABI we care about.
     */
    public static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("name"),
            ValueLayout.JAVA_INT.withName("version"),
            ValueLayout.JAVA_INT.withName("method_count"),
            ValueLayout.ADDRESS.withName("methods"),
            ValueLayout.JAVA_INT.withName("event_count"),
            MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS.withName("events")
    ).withName("wl_interface");

    private static final VarHandle NAME_VH = LAYOUT.varHandle(PathElement.groupElement("name"));
    private static final VarHandle VERSION_VH = LAYOUT.varHandle(PathElement.groupElement("version"));
    private static final VarHandle METHOD_COUNT_VH = LAYOUT.varHandle(PathElement.groupElement("method_count"));
    private static final VarHandle METHODS_VH = LAYOUT.varHandle(PathElement.groupElement("methods"));
    private static final VarHandle EVENT_COUNT_VH = LAYOUT.varHandle(PathElement.groupElement("event_count"));
    private static final VarHandle EVENTS_VH = LAYOUT.varHandle(PathElement.groupElement("events"));

    private final MemorySegment segment;

    private WlInterface(MemorySegment segment) {
        this.segment = Objects.requireNonNull(segment);
    }

    /** Wrap an existing pointer. The segment is reinterpreted to {@link #LAYOUT}'s size. */
    public static WlInterface wrap(MemorySegment ptr) {
        if (ptr == null || ptr.address() == 0L) {
            throw new WaylandClientException("Cannot wrap NULL wl_interface pointer");
        }
        return new WlInterface(ptr.reinterpret(LAYOUT.byteSize()));
    }

    /**
     * Resolve a global {@code wl_interface} symbol from
     * {@code libwayland-client}, e.g. {@code "wl_display_interface"}.
     */
    public static Optional<WlInterface> lookup(String symbol) {
        return WaylandNative.findSymbol(symbol).map(WlInterface::wrap);
    }

    /** The C address of this {@code wl_interface} struct. */
    public MemorySegment address() {
        return segment;
    }

    /** The interface's protocol name (e.g. {@code "wl_surface"}). */
    public String name() {
        return WaylandNative.readCString((MemorySegment) NAME_VH.get(segment, 0L));
    }

    /** The maximum supported version of this interface. */
    public int version() {
        return (int) VERSION_VH.get(segment, 0L);
    }

    public int methodCount() {
        return (int) METHOD_COUNT_VH.get(segment, 0L);
    }

    public int eventCount() {
        return (int) EVENT_COUNT_VH.get(segment, 0L);
    }

    /** Read the {@code methods[index]} entry. */
    public WlMessage method(int index) {
        return readMessage(METHODS_VH, methodCount(), index);
    }

    /** Read the {@code events[index]} entry. */
    public WlMessage event(int index) {
        return readMessage(EVENTS_VH, eventCount(), index);
    }

    private WlMessage readMessage(VarHandle arrayPtrVh, int count, int index) {
        if (index < 0 || index >= count) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds [0," + count + ")");
        }
        MemorySegment arrayPtr = (MemorySegment) arrayPtrVh.get(segment, 0L);
        if (arrayPtr.address() == 0L) {
            throw new WaylandClientException("wl_interface array pointer is NULL");
        }
        long off = (long) index * WlMessage.LAYOUT.byteSize();
        MemorySegment one = arrayPtr.reinterpret(off + WlMessage.LAYOUT.byteSize())
                .asSlice(off, WlMessage.LAYOUT.byteSize());
        return WlMessage.wrap(one);
    }

    /**
     * Allocate a new {@code wl_interface} in {@code arena}. {@code methods}
     * and {@code events} may be empty. The supplied {@link WlMessage}
     * objects are <em>copied</em> into freshly allocated arrays in
     * {@code arena}; the originals are not retained.
     */
    public static WlInterface allocate(
            Arena arena,
            String name,
            int version,
            List<WlMessage> methods,
            List<WlMessage> events) {
        MemorySegment seg = arena.allocate(LAYOUT);
        NAME_VH.set(seg, 0L, arena.allocateFrom(name));
        VERSION_VH.set(seg, 0L, version);
        METHOD_COUNT_VH.set(seg, 0L, methods.size());
        METHODS_VH.set(seg, 0L, copyMessages(arena, methods));
        EVENT_COUNT_VH.set(seg, 0L, events.size());
        EVENTS_VH.set(seg, 0L, copyMessages(arena, events));
        return new WlInterface(seg);
    }

    private static MemorySegment copyMessages(Arena arena, List<WlMessage> src) {
        if (src.isEmpty()) {
            return MemorySegment.NULL;
        }
        SequenceLayout seq = MemoryLayout.sequenceLayout(src.size(), WlMessage.LAYOUT);
        MemorySegment array = arena.allocate(seq);
        long stride = WlMessage.LAYOUT.byteSize();
        for (int i = 0; i < src.size(); i++) {
            MemorySegment.copy(src.get(i).address(), 0L, array, i * stride, stride);
        }
        return array;
    }
}
