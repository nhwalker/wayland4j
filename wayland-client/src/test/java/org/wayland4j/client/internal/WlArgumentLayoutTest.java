package org.wayland4j.client.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WlArgumentLayoutTest {

    @Test
    void fixedToDoubleRoundTripsExactPowerOfTwoFractions() {
        // 24.8 fixed point: 256 = 1.0, 128 = 0.5, etc.
        assertEquals(0.0, WlArgumentLayout.fixedToDouble(0));
        assertEquals(1.0, WlArgumentLayout.fixedToDouble(256));
        assertEquals(-1.0, WlArgumentLayout.fixedToDouble(-256));
        assertEquals(0.5, WlArgumentLayout.fixedToDouble(128));
        assertEquals(-0.5, WlArgumentLayout.fixedToDouble(-128));
        assertEquals(1.0 / 256.0, WlArgumentLayout.fixedToDouble(1));
    }

    @Test
    void doubleToFixedRoundsToNearest() {
        assertEquals(0, WlArgumentLayout.doubleToFixed(0.0));
        assertEquals(256, WlArgumentLayout.doubleToFixed(1.0));
        assertEquals(-256, WlArgumentLayout.doubleToFixed(-1.0));
        assertEquals(128, WlArgumentLayout.doubleToFixed(0.5));
        assertEquals(1, WlArgumentLayout.doubleToFixed(1.0 / 256.0));
        // Halfway case rounds half-up via Math.round (per JLS).
        assertEquals(1, WlArgumentLayout.doubleToFixed(1.0 / 512.0));
    }

    @Test
    void roundTripPreservesRepresentableValues() {
        for (int raw : new int[] {0, 1, -1, 256, -256, 12345, -12345, Integer.MAX_VALUE / 2, Integer.MIN_VALUE / 2}) {
            assertEquals(raw, WlArgumentLayout.doubleToFixed(WlArgumentLayout.fixedToDouble(raw)),
                    "round-trip for raw=" + raw);
        }
    }
}
