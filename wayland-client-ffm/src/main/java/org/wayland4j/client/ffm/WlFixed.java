package org.wayland4j.client.ffm;

/**
 * Conversions between Java numbers and {@code wl_fixed_t}, a signed
 * 24.8 fixed-point integer that is the only non-trivial scalar in the
 * Wayland wire protocol.
 *
 * <p>libwayland implements these conversions as C macros in
 * {@code <wayland-util.h>}. Implementations there use a double bit-pattern
 * trick that is not portable to Java; the straightforward shift / multiply
 * implementations below produce identical results for all defined inputs.
 */
public final class WlFixed {

    private WlFixed() {}

    /** Number of fractional bits in {@code wl_fixed_t}. */
    public static final int FRACTIONAL_BITS = 8;

    /** Multiplier between integer and fixed representations ({@code 1 << 8 == 256}). */
    public static final int ONE = 1 << FRACTIONAL_BITS;

    /** Convert a Java {@code int} to {@code wl_fixed_t}. */
    public static int fromInt(int value) {
        return value << FRACTIONAL_BITS;
    }

    /** Convert a Java {@code double} to {@code wl_fixed_t} (rounds toward zero). */
    public static int fromDouble(double value) {
        return (int) (value * ONE);
    }

    /** Truncate a {@code wl_fixed_t} to a Java {@code int}. */
    public static int toInt(int wlFixed) {
        return wlFixed >> FRACTIONAL_BITS;
    }

    /** Convert a {@code wl_fixed_t} to a Java {@code double}. */
    public static double toDouble(int wlFixed) {
        return wlFixed / (double) ONE;
    }
}
