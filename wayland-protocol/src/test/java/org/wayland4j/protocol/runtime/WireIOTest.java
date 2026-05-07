package org.wayland4j.protocol.runtime;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WireIOTest {

    @Test
    void intRoundTripLittleEndian() throws Exception {
        var out = new ByteArrayOutputStream();
        WireIO.writeInt(out, 0x01020304);
        assertArrayEquals(new byte[] {0x04, 0x03, 0x02, 0x01}, out.toByteArray());
        assertEquals(0x01020304, WireIO.readInt(new ByteArrayInputStream(out.toByteArray())));
    }

    @Test
    void uintTreatedAsUnsigned() throws Exception {
        var out = new ByteArrayOutputStream();
        WireIO.writeUInt(out, 0xffffffff);
        assertEquals(0xffffffff, WireIO.readUInt(new ByteArrayInputStream(out.toByteArray())));
    }

    @Test
    void fixedRoundTrip() {
        assertEquals(256, WireIO.doubleToFixed(1.0));
        assertEquals(-256, WireIO.doubleToFixed(-1.0));
        assertEquals(1.0, WireIO.fixedToDouble(256));
        assertEquals(-0.5, WireIO.fixedToDouble(-128));
    }

    @Test
    void stringEmpty() throws Exception {
        var out = new ByteArrayOutputStream();
        WireIO.writeString(out, "");
        // length=1 (just NUL), 1 byte body, 3 bytes pad => 4 + 4 = 8 bytes total
        assertArrayEquals(new byte[] {1, 0, 0, 0, 0, 0, 0, 0}, out.toByteArray());
        assertEquals("", WireIO.readString(new ByteArrayInputStream(out.toByteArray())));
    }

    @Test
    void stringNonEmptyPadded() throws Exception {
        var out = new ByteArrayOutputStream();
        WireIO.writeString(out, "hi");
        // length=3 (h,i,\0), 3 bytes body, 1 pad => 4 + 4 = 8 bytes
        assertArrayEquals(
                new byte[] {3, 0, 0, 0, 'h', 'i', 0, 0},
                out.toByteArray());
        assertEquals("hi", WireIO.readString(new ByteArrayInputStream(out.toByteArray())));
    }

    @Test
    void stringFourBytePayloadWithNulYieldsZeroPadding() throws Exception {
        var out = new ByteArrayOutputStream();
        WireIO.writeString(out, "abc");
        // length=4, 4 bytes body, 0 pad => 4 + 4 = 8 bytes
        assertArrayEquals(
                new byte[] {4, 0, 0, 0, 'a', 'b', 'c', 0},
                out.toByteArray());
        assertEquals("abc", WireIO.readString(new ByteArrayInputStream(out.toByteArray())));
    }

    @Test
    void stringNullableEncodesNullAsZeroLength() throws Exception {
        var out = new ByteArrayOutputStream();
        WireIO.writeStringNullable(out, null);
        assertArrayEquals(new byte[] {0, 0, 0, 0}, out.toByteArray());
        assertNull(WireIO.readStringNullable(new ByteArrayInputStream(out.toByteArray())));
    }

    @Test
    void readStringRejectsNull() throws Exception {
        var bytes = new byte[] {0, 0, 0, 0};
        assertThrows(WireFormatException.class,
                () -> WireIO.readString(new ByteArrayInputStream(bytes)));
    }

    @Test
    void arrayPaddedToFour() throws Exception {
        var out = new ByteArrayOutputStream();
        WireIO.writeArray(out, new byte[] {1, 2, 3, 4, 5});
        // length=5, 5 bytes, 3 pad => 4 + 8 = 12 bytes
        assertArrayEquals(
                new byte[] {5, 0, 0, 0, 1, 2, 3, 4, 5, 0, 0, 0},
                out.toByteArray());
        assertArrayEquals(
                new byte[] {1, 2, 3, 4, 5},
                WireIO.readArray(new ByteArrayInputStream(out.toByteArray())));
    }

    @Test
    void emptyArrayHasNoPadding() throws Exception {
        var out = new ByteArrayOutputStream();
        WireIO.writeArray(out, new byte[0]);
        assertArrayEquals(new byte[] {0, 0, 0, 0}, out.toByteArray());
        assertArrayEquals(new byte[0], WireIO.readArray(new ByteArrayInputStream(out.toByteArray())));
    }

    @Test
    void objectRejectsZero() {
        assertThrows(IllegalArgumentException.class,
                () -> WireIO.writeObject(new ByteArrayOutputStream(), 0));
        assertThrows(WireFormatException.class,
                () -> WireIO.readObject(new ByteArrayInputStream(new byte[] {0, 0, 0, 0})));
    }

    @Test
    void objectNullableAcceptsZero() throws Exception {
        var out = new ByteArrayOutputStream();
        WireIO.writeObjectNullable(out, 0);
        assertEquals(0, WireIO.readObjectNullable(new ByteArrayInputStream(out.toByteArray())));
    }

    @Test
    void headerPackUnpack() {
        int sizeOpcode = WireIO.packSizeOpcode(0x1234, 0x0007);
        assertEquals(0x1234, WireIO.headerSize(sizeOpcode));
        assertEquals(0x0007, WireIO.headerOpcode(sizeOpcode));
    }

    @Test
    void headerRejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> WireIO.packSizeOpcode(0x10000, 1));
        assertThrows(IllegalArgumentException.class,
                () -> WireIO.packSizeOpcode(8, 0x10000));
    }

    @Test
    void shortReadOnIntFails() {
        assertThrows(EOFException.class,
                () -> WireIO.readInt(new ByteArrayInputStream(new byte[] {1, 2})));
    }

    @Test
    void padToFourMath() {
        assertEquals(0, WireIO.padTo4(0));
        assertEquals(4, WireIO.padTo4(1));
        assertEquals(4, WireIO.padTo4(4));
        assertEquals(8, WireIO.padTo4(5));
    }
}
