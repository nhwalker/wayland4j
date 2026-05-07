package org.wayland4j.client.ffm;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;

/**
 * Layout and accessor for {@code struct wl_array}:
 * <pre>{@code
 * struct wl_array {
 *     size_t size;
 *     size_t alloc;
 *     void *data;
 * };
 * }</pre>
 *
 * <p>Wayland passes arrays of bytes by reference using this struct (see the
 * {@code 'a'} signature character). The library owns the allocation; on the
 * read side the only useful operations are reading {@code size} and copying
 * the payload out via {@link #copyOut(MemorySegment)}.
 */
public final class WlArray {

    private WlArray() {}

    public static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            WaylandNative.SIZE_T.withName("size"),
            WaylandNative.SIZE_T.withName("alloc"),
            ValueLayout.ADDRESS.withName("data")
    ).withName("wl_array");

    private static final VarHandle SIZE_VH = LAYOUT.varHandle(PathElement.groupElement("size"));
    private static final VarHandle ALLOC_VH = LAYOUT.varHandle(PathElement.groupElement("alloc"));
    private static final VarHandle DATA_VH = LAYOUT.varHandle(PathElement.groupElement("data"));

    /** Allocate a zero-initialized {@code wl_array} in {@code allocator}. */
    public static MemorySegment allocate(SegmentAllocator allocator) {
        return allocator.allocate(LAYOUT);
    }

    /** {@code wl_array.size}, in bytes. */
    public static long size(MemorySegment array) {
        return (long) SIZE_VH.get(array, 0L);
    }

    /** {@code wl_array.alloc}, in bytes (capacity). */
    public static long alloc(MemorySegment array) {
        return (long) ALLOC_VH.get(array, 0L);
    }

    /**
     * The {@code wl_array.data} pointer, reinterpreted to span exactly
     * {@code wl_array.size} bytes. The returned segment shares its scope
     * with {@code array}.
     */
    public static MemorySegment data(MemorySegment array) {
        MemorySegment ptr = (MemorySegment) DATA_VH.get(array, 0L);
        long n = size(array);
        if (ptr.address() == 0L || n == 0L) {
            return MemorySegment.NULL;
        }
        return ptr.reinterpret(n);
    }

    /** Copy the array contents into a fresh Java {@code byte[]}. */
    public static byte[] copyOut(MemorySegment array) {
        long n = size(array);
        if (n == 0L) {
            return new byte[0];
        }
        if (n > Integer.MAX_VALUE) {
            throw new WaylandClientException(
                    "wl_array of " + n + " bytes is too large for a Java byte[]");
        }
        return data(array).toArray(ValueLayout.JAVA_BYTE);
    }
}
