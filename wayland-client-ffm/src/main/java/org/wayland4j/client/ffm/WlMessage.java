package org.wayland4j.client.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Objects;

/**
 * Wrapper for {@code struct wl_message}:
 * <pre>{@code
 * struct wl_message {
 *     const char *name;
 *     const char *signature;
 *     const struct wl_interface **types;
 * };
 * }</pre>
 *
 * <p>libwayland exposes one of these per request and per event of every
 * interface. The struct itself is small and immutable; this class wraps the
 * pointer and parses the signature on demand.
 */
public final class WlMessage {

    /** Layout of a single {@code wl_message} struct. */
    public static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("name"),
            ValueLayout.ADDRESS.withName("signature"),
            ValueLayout.ADDRESS.withName("types")
    ).withName("wl_message");

    private static final VarHandle NAME_VH = LAYOUT.varHandle(PathElement.groupElement("name"));
    private static final VarHandle SIGNATURE_VH = LAYOUT.varHandle(PathElement.groupElement("signature"));
    private static final VarHandle TYPES_VH = LAYOUT.varHandle(PathElement.groupElement("types"));

    private final MemorySegment segment;

    private WlMessage(MemorySegment segment) {
        this.segment = Objects.requireNonNull(segment);
    }

    /** Wrap an existing pointer. The segment is reinterpreted to {@link #LAYOUT}'s size. */
    public static WlMessage wrap(MemorySegment ptr) {
        if (ptr == null || ptr.address() == 0L) {
            throw new WaylandClientException("Cannot wrap NULL wl_message pointer");
        }
        return new WlMessage(ptr.reinterpret(LAYOUT.byteSize()));
    }

    /** The C address of this {@code wl_message} struct. */
    public MemorySegment address() {
        return segment;
    }

    /** The {@code name} field (request/event name as in the protocol XML). */
    public String name() {
        return WaylandNative.readCString((MemorySegment) NAME_VH.get(segment, 0L));
    }

    /** The raw {@code signature} field, including any leading version digits and {@code '?'} markers. */
    public String signature() {
        return WaylandNative.readCString((MemorySegment) SIGNATURE_VH.get(segment, 0L));
    }

    /** Parse {@link #signature()} into structured argument descriptors. */
    public List<WlArgumentType.Element> arguments() {
        return WlArgumentType.parse(signature());
    }

    /**
     * Pointer to the {@code wl_interface*} array that resolves
     * {@code object} / {@code new_id} arguments. May be NULL when the
     * signature contains no such arguments.
     */
    public MemorySegment typesPointer() {
        return (MemorySegment) TYPES_VH.get(segment, 0L);
    }

    /**
     * Allocate a new {@code wl_message} in {@code arena} populated with the
     * supplied fields. Strings are copied into {@code arena} as
     * NUL-terminated UTF-8. {@code typesArray} should already be a pointer
     * to a {@code wl_interface*[]} of length matching the parsed signature
     * (typically allocated by {@code WlInterface.builder(...)}).
     */
    public static WlMessage allocate(
            Arena arena,
            String name,
            String signature,
            MemorySegment typesArray) {
        MemorySegment seg = arena.allocate(LAYOUT);
        NAME_VH.set(seg, 0L, arena.allocateFrom(name));
        SIGNATURE_VH.set(seg, 0L, arena.allocateFrom(signature));
        TYPES_VH.set(seg, 0L, typesArray == null ? MemorySegment.NULL : typesArray);
        return new WlMessage(seg);
    }
}
