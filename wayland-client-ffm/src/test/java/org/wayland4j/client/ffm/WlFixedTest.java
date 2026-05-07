package org.wayland4j.client.ffm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WlFixedTest {

    @Test
    void zero() {
        assertEquals(0, WlFixed.fromInt(0));
        assertEquals(0, WlFixed.fromDouble(0.0));
        assertEquals(0, WlFixed.toInt(0));
        assertEquals(0.0, WlFixed.toDouble(0));
    }

    @Test
    void integerRoundTrip() {
        for (int v : new int[]{-1024, -1, 1, 7, 256, 65535, (1 << 23) - 1, -(1 << 23)}) {
            int fixed = WlFixed.fromInt(v);
            assertEquals(v, WlFixed.toInt(fixed), "round-trip for " + v);
        }
    }

    @Test
    void doubleConversionMatchesUpstream() {
        // From <wayland-util.h>: 1.5 → 0x180 (1 * 256 + 128).
        assertEquals(0x180, WlFixed.fromDouble(1.5));
        assertEquals(0x100, WlFixed.fromDouble(1.0));
        assertEquals(-0x80, WlFixed.fromDouble(-0.5));
        assertEquals(1.5, WlFixed.toDouble(0x180));
        assertEquals(-0.5, WlFixed.toDouble(-0x80));
    }

    @Test
    void truncationMatchesShift() {
        // toInt should be arithmetic shift, not divide — so negative
        // truncation rounds toward negative infinity.
        assertEquals(-1, WlFixed.toInt(-1));      // any negative fractional → -1
        assertEquals(-1, WlFixed.toInt(-128));    // -0.5 → -1
        assertEquals(0, WlFixed.toInt(127));      // +0.49 → 0
    }

    @Test
    void oneConstant() {
        assertEquals(256, WlFixed.ONE);
        assertEquals(WlFixed.fromInt(1), WlFixed.ONE);
    }
}
