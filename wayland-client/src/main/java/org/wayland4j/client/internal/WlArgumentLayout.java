package org.wayland4j.client.internal;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

/**
 * Layout of {@code union wl_argument} (8 bytes per slot on LP64) and helpers
 * for reading/writing slots.
 */
public final class WlArgumentLayout {

    public static final long SLOT_SIZE = 8L;
    public static final ValueLayout.OfInt INT = ValueLayout.JAVA_INT;
    public static final ValueLayout.OfLong LONG = ValueLayout.JAVA_LONG;
    public static final AddressLayout ADDRESS = ValueLayout.ADDRESS;
    public static final MemoryLayout SLOT = MemoryLayout.unionLayout(
            ValueLayout.JAVA_INT.withName("i"),
            ValueLayout.JAVA_INT.withName("u"),
            ValueLayout.JAVA_INT.withName("f"),
            ValueLayout.ADDRESS.withName("s"),
            ValueLayout.ADDRESS.withName("o"),
            ValueLayout.JAVA_INT.withName("n"),
            ValueLayout.ADDRESS.withName("a"),
            ValueLayout.JAVA_INT.withName("h")
    ).withName("wl_argument");

    private WlArgumentLayout() {
    }

    public static long offset(int slot) {
        return slot * SLOT_SIZE;
    }

    public static int readInt(MemorySegment args, int slot) {
        return args.get(INT, offset(slot));
    }

    public static long readAddress(MemorySegment args, int slot) {
        return args.get(ADDRESS, offset(slot)).address();
    }

    public static String readString(MemorySegment args, int slot) {
        long addr = readAddress(args, slot);
        if (addr == 0L) return null;
        MemorySegment cstr = MemorySegment.ofAddress(addr).reinterpret(Long.MAX_VALUE);
        return cstr.getString(0, StandardCharsets.UTF_8);
    }

    public static byte[] readArray(MemorySegment args, int slot) {
        long addr = readAddress(args, slot);
        if (addr == 0L) return null;
        // wl_array { size_t size; size_t alloc; void *data; }
        MemorySegment header = MemorySegment.ofAddress(addr).reinterpret(24);
        long size = header.get(LONG, 0);
        long dataAddr = header.get(ADDRESS, 16).address();
        if (dataAddr == 0L || size == 0L) return new byte[0];
        MemorySegment data = MemorySegment.ofAddress(dataAddr).reinterpret(size);
        byte[] out = new byte[(int) size];
        MemorySegment.copy(data, 0, MemorySegment.ofArray(out), 0, size);
        return out;
    }

    public static void writeInt(MemorySegment args, int slot, int value) {
        args.set(INT, offset(slot), value);
    }

    public static void writeAddress(MemorySegment args, int slot, MemorySegment ptr) {
        args.set(ADDRESS, offset(slot), ptr == null ? MemorySegment.NULL : ptr);
    }

    /** {@code wl_fixed_t} -> double. Wire format is signed 24.8 fixed point. */
    public static double fixedToDouble(int raw) {
        return raw / 256.0;
    }

    public static int doubleToFixed(double v) {
        return (int) Math.round(v * 256.0);
    }
}
