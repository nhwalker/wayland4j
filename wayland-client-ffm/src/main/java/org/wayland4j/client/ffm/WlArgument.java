package org.wayland4j.client.ffm;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.UnionLayout;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Java mirror of {@code union wl_argument}, the 8-byte tagged-union
 * representation of a single Wayland argument:
 * <pre>{@code
 * union wl_argument {
 *     int32_t i;          // signature 'i'
 *     uint32_t u;         // signature 'u'
 *     wl_fixed_t f;       // signature 'f'
 *     const char *s;      // signature 's'
 *     struct wl_object *o;// signature 'o'
 *     uint32_t n;         // signature 'n'
 *     struct wl_array *a; // signature 'a'
 *     int32_t h;          // signature 'h'
 * };
 * }</pre>
 *
 * <p>The matching Java types are {@code record}s implementing this sealed
 * interface. Two utility methods convert to and from the C array
 * representation: {@link #write} for outgoing requests and
 * {@link #read} for the {@code args} parameter passed to a
 * {@link WlDispatcher} on incoming events.
 */
public sealed interface WlArgument {

    /** Layout of one {@code union wl_argument} slot. */
    UnionLayout LAYOUT = MemoryLayout.unionLayout(
            ValueLayout.JAVA_INT.withName("i"),
            ValueLayout.JAVA_INT.withName("u"),
            ValueLayout.JAVA_INT.withName("f"),
            ValueLayout.ADDRESS.withName("s"),
            ValueLayout.ADDRESS.withName("o"),
            ValueLayout.JAVA_INT.withName("n"),
            ValueLayout.ADDRESS.withName("a"),
            ValueLayout.JAVA_INT.withName("h")
    ).withName("wl_argument");

    /** {@code 'i'} — signed 32-bit int. */
    record IntArg(int value) implements WlArgument {}

    /** {@code 'u'} — unsigned 32-bit int. */
    record UIntArg(int value) implements WlArgument {}

    /**
     * {@code 'f'} — {@code wl_fixed_t} (24.8 fixed-point). Use
     * {@link WlFixed} to convert to/from real numbers.
     */
    record FixedArg(int wlFixed) implements WlArgument {}

    /** {@code 's'} — UTF-8 string (or null). */
    record StringArg(String value) implements WlArgument {}

    /**
     * {@code 'o'} — Wayland object reference. Holds a raw proxy pointer;
     * the higher-level {@link WlProxy} wrapper, if any, must be looked up
     * via the proxy's user data.
     */
    record ObjectArg(MemorySegment proxy) implements WlArgument {}

    /** {@code 'n'} — newly allocated id. */
    record NewIdArg(int id) implements WlArgument {}

    /**
     * {@code 'a'} — pointer to a {@code wl_array}. Use {@link WlArray} to
     * read fields of the pointee.
     */
    record ArrayArg(MemorySegment array) implements WlArgument {}

    /** {@code 'h'} — file descriptor. */
    record FdArg(int fd) implements WlArgument {}

    /**
     * Allocate a {@code union wl_argument[]} in {@code allocator} matching
     * the supplied list. Strings are copied into {@code allocator} as
     * NUL-terminated UTF-8.
     *
     * @return the address of the first slot, suitable for passing to
     *         {@code wl_proxy_marshal_array_flags}
     */
    static MemorySegment write(SegmentAllocator allocator, List<? extends WlArgument> args) {
        if (args.isEmpty()) {
            return MemorySegment.NULL;
        }
        long stride = LAYOUT.byteSize();
        MemorySegment array = allocator.allocate(MemoryLayout.sequenceLayout(args.size(), LAYOUT));
        for (int i = 0; i < args.size(); i++) {
            long off = i * stride;
            WlArgument a = args.get(i);
            switch (a) {
                case IntArg ia -> array.set(ValueLayout.JAVA_INT, off, ia.value);
                case UIntArg ua -> array.set(ValueLayout.JAVA_INT, off, ua.value);
                case FixedArg fa -> array.set(ValueLayout.JAVA_INT, off, fa.wlFixed);
                case StringArg sa -> {
                    MemorySegment ptr = sa.value == null
                            ? MemorySegment.NULL
                            : allocator.allocateFrom(sa.value);
                    array.set(ValueLayout.ADDRESS, off, ptr);
                }
                case ObjectArg oa -> array.set(ValueLayout.ADDRESS, off,
                        oa.proxy == null ? MemorySegment.NULL : oa.proxy);
                case NewIdArg na -> array.set(ValueLayout.JAVA_INT, off, na.id);
                case ArrayArg aa -> array.set(ValueLayout.ADDRESS, off,
                        aa.array == null ? MemorySegment.NULL : aa.array);
                case FdArg fda -> array.set(ValueLayout.JAVA_INT, off, fda.fd);
            }
        }
        return array;
    }

    /**
     * Decode a {@code union wl_argument[]} according to {@code signature}.
     * Used from {@link WlDispatcher#dispatch} to turn the raw native
     * arguments into typed Java values.
     */
    static List<WlArgument> read(MemorySegment args, List<WlArgumentType.Element> signature) {
        if (signature.isEmpty()) {
            return List.of();
        }
        if (args == null || args.address() == 0L) {
            throw new WaylandClientException(
                    "wl_argument array is NULL but signature has " + signature.size() + " entries");
        }
        long stride = LAYOUT.byteSize();
        MemorySegment view = args.byteSize() >= stride * signature.size()
                ? args
                : args.reinterpret(stride * signature.size());
        List<WlArgument> out = new ArrayList<>(signature.size());
        for (int i = 0; i < signature.size(); i++) {
            long off = i * stride;
            WlArgumentType type = signature.get(i).type();
            switch (type) {
                case INT -> out.add(new IntArg(view.get(ValueLayout.JAVA_INT, off)));
                case UINT -> out.add(new UIntArg(view.get(ValueLayout.JAVA_INT, off)));
                case FIXED -> out.add(new FixedArg(view.get(ValueLayout.JAVA_INT, off)));
                case STRING -> out.add(new StringArg(
                        WaylandNative.readCString(view.get(ValueLayout.ADDRESS, off))));
                case OBJECT -> out.add(new ObjectArg(view.get(ValueLayout.ADDRESS, off)));
                case NEW_ID -> out.add(new NewIdArg(view.get(ValueLayout.JAVA_INT, off)));
                case ARRAY -> out.add(new ArrayArg(view.get(ValueLayout.ADDRESS, off)));
                case FD -> out.add(new FdArg(view.get(ValueLayout.JAVA_INT, off)));
            }
        }
        return List.copyOf(out);
    }
}
