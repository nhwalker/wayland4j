package org.wayland4j.client.ffm;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;

/**
 * Layout descriptor for {@code struct wl_list} from {@code <wayland-util.h>}:
 * an intrusive doubly-linked list head with a {@code prev} and {@code next}
 * pointer. Wayland uses these in a few places (e.g. {@code wl_array_for_each}
 * tail) but most clients never look at them directly. We model only the
 * layout for now; the manipulation helpers ({@code wl_list_init},
 * {@code wl_list_insert}, etc.) can be exposed when a real consumer needs
 * them.
 */
public final class WlList {

    private WlList() {}

    public static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("prev"),
            ValueLayout.ADDRESS.withName("next")
    ).withName("wl_list");

    /** Allocate a zero-initialized {@code wl_list} in {@code allocator}. */
    public static MemorySegment allocate(SegmentAllocator allocator) {
        return allocator.allocate(LAYOUT);
    }
}
