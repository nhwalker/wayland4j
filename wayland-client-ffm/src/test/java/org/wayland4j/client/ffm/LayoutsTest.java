package org.wayland4j.client.ffm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.foreign.ValueLayout;
import org.junit.jupiter.api.Test;

/**
 * Pin the byte-sizes of the Wayland C structs we model. These constants
 * are the upstream values on a 64-bit ABI; if they ever change, libwayland
 * has broken its ABI and our generated code will already be unusable.
 */
class LayoutsTest {

    private static final long PTR = ValueLayout.ADDRESS.byteSize();

    @Test
    void wlMessageSize() {
        // 3 pointers: name, signature, types
        assertEquals(3 * PTR, WlMessage.LAYOUT.byteSize());
    }

    @Test
    void wlInterfaceSize() {
        // ptr + int + int + ptr + int + 4 padding + ptr  ==  40 on 64-bit
        long expected = PTR + 4 + 4 + PTR + 4 + 4 + PTR;
        assertEquals(expected, WlInterface.LAYOUT.byteSize());
    }

    @Test
    void wlListSize() {
        // 2 pointers
        assertEquals(2 * PTR, WlList.LAYOUT.byteSize());
    }

    @Test
    void wlArraySize() {
        // 2 size_t + 1 pointer
        long sizeT = WaylandNative.SIZE_T.byteSize();
        assertEquals(sizeT * 2 + PTR, WlArray.LAYOUT.byteSize());
    }

    @Test
    void wlArgumentSize() {
        // union: max member is a pointer (8 bytes on 64-bit)
        assertEquals(PTR, WlArgument.LAYOUT.byteSize());
    }
}
