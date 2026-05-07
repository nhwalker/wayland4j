package org.wayland4j.protocol.runtime;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Codec primitives for the Wayland wire format.
 *
 * <p>All multi-byte integers are little-endian. The C reference
 * implementation uses host byte order on the local Unix socket, which is LE on
 * every platform Wayland is deployed on today. We lock to LE here so generated
 * codecs are byte-deterministic and testable in isolation; revisit only if a
 * BE host ever becomes a target.
 *
 * <p>Layout reference:
 * <a href="https://wayland-book.com/protocol-design/wire-protocol.html">
 * The Wayland Book — wire protocol</a>.
 */
public final class WireIO {

    private WireIO() {}

    private static final byte[] PAD_ZEROS = new byte[3];

    public static int padTo4(int n) {
        return (n + 3) & ~3;
    }

    // ---- 32-bit integers --------------------------------------------------

    public static int readInt(InputStream in) throws IOException {
        byte[] buf = readExactly(in, 4);
        return  (buf[0] & 0xff)
             | ((buf[1] & 0xff) << 8)
             | ((buf[2] & 0xff) << 16)
             | ((buf[3] & 0xff) << 24);
    }

    public static void writeInt(OutputStream out, int v) throws IOException {
        out.write(v        & 0xff);
        out.write((v >>> 8)  & 0xff);
        out.write((v >>> 16) & 0xff);
        out.write((v >>> 24) & 0xff);
    }

    /** Reads a u32 LE; returned as {@code int}, treat as unsigned. */
    public static int readUInt(InputStream in) throws IOException {
        return readInt(in);
    }

    /** Writes a u32 LE; the {@code int} is treated as unsigned. */
    public static void writeUInt(OutputStream out, int v) throws IOException {
        writeInt(out, v);
    }

    // ---- fixed (24.8 signed) ---------------------------------------------

    public static int readFixedRaw(InputStream in) throws IOException {
        return readInt(in);
    }

    public static void writeFixedRaw(OutputStream out, int raw) throws IOException {
        writeInt(out, raw);
    }

    public static double fixedToDouble(int raw) {
        return raw / 256.0;
    }

    public static int doubleToFixed(double v) {
        return (int) Math.round(v * 256.0);
    }

    // ---- string (length includes NUL, padded to 4) -----------------------

    /** Reads a non-null string; throws if the wire indicates null (length 0). */
    public static String readString(InputStream in) throws IOException {
        String s = readStringNullable(in);
        if (s == null) {
            throw new WireFormatException("null string where non-null was expected");
        }
        return s;
    }

    /** Writes a non-null string. Length on wire = utf8.length + 1, padded to 4. */
    public static void writeString(OutputStream out, String s) throws IOException {
        if (s == null) {
            throw new NullPointerException("non-null string required");
        }
        writeStringPayload(out, s);
    }

    /** Reads a possibly-null string. Length 0 on the wire encodes null. */
    public static String readStringNullable(InputStream in) throws IOException {
        int length = readInt(in);
        if (length == 0) {
            return null;
        }
        if (length < 0) {
            throw new WireFormatException("string length out of range: " + Integer.toUnsignedString(length));
        }
        byte[] body = readExactly(in, length);
        skipExactly(in, padTo4(length) - length);
        if (body[length - 1] != 0) {
            throw new WireFormatException("string missing NUL terminator");
        }
        return new String(body, 0, length - 1, StandardCharsets.UTF_8);
    }

    public static void writeStringNullable(OutputStream out, String s) throws IOException {
        if (s == null) {
            writeInt(out, 0);
            return;
        }
        writeStringPayload(out, s);
    }

    private static void writeStringPayload(OutputStream out, String s) throws IOException {
        byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
        int length = utf8.length + 1;
        writeInt(out, length);
        out.write(utf8);
        out.write(0);
        int pad = padTo4(length) - length;
        if (pad > 0) out.write(PAD_ZEROS, 0, pad);
    }

    // ---- array (length-prefixed bytes, padded to 4) ----------------------

    public static byte[] readArray(InputStream in) throws IOException {
        int length = readInt(in);
        if (length < 0) {
            throw new WireFormatException("array length out of range: " + Integer.toUnsignedString(length));
        }
        byte[] body = readExactly(in, length);
        skipExactly(in, padTo4(length) - length);
        return body;
    }

    public static void writeArray(OutputStream out, byte[] bytes) throws IOException {
        if (bytes == null) {
            throw new NullPointerException("array must not be null (use empty array for length 0)");
        }
        writeInt(out, bytes.length);
        out.write(bytes);
        int pad = padTo4(bytes.length) - bytes.length;
        if (pad > 0) out.write(PAD_ZEROS, 0, pad);
    }

    // ---- object / new_id -------------------------------------------------

    /** Reads a non-null object id (u32). 0 is illegal here. */
    public static int readObject(InputStream in) throws IOException {
        int id = readInt(in);
        if (id == 0) {
            throw new WireFormatException("null object id where non-null was expected");
        }
        return id;
    }

    public static void writeObject(OutputStream out, int id) throws IOException {
        if (id == 0) {
            throw new IllegalArgumentException("object id must be non-zero");
        }
        writeInt(out, id);
    }

    /** Reads a possibly-null object id. 0 encodes null. */
    public static int readObjectNullable(InputStream in) throws IOException {
        return readInt(in);
    }

    public static void writeObjectNullable(OutputStream out, int id) throws IOException {
        writeInt(out, id);
    }

    public static int readNewId(InputStream in) throws IOException {
        return readObject(in);
    }

    public static void writeNewId(OutputStream out, int id) throws IOException {
        writeObject(out, id);
    }

    // ---- header packing --------------------------------------------------

    /**
     * Packs the second header u32: high 16 bits = total message size in bytes
     * (header included), low 16 bits = opcode.
     */
    public static int packSizeOpcode(int size, int opcode) {
        if ((size & ~0xffff) != 0) {
            throw new IllegalArgumentException("size out of u16 range: " + size);
        }
        if ((opcode & ~0xffff) != 0) {
            throw new IllegalArgumentException("opcode out of u16 range: " + opcode);
        }
        return (size << 16) | (opcode & 0xffff);
    }

    public static int headerSize(int sizeOpcode) {
        return (sizeOpcode >>> 16) & 0xffff;
    }

    public static int headerOpcode(int sizeOpcode) {
        return sizeOpcode & 0xffff;
    }

    // ---- low-level IO helpers --------------------------------------------

    private static byte[] readExactly(InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int read = in.readNBytes(buf, 0, n);
        if (read != n) {
            throw new EOFException("expected " + n + " bytes, got " + read);
        }
        return buf;
    }

    private static void skipExactly(InputStream in, int n) throws IOException {
        if (n == 0) return;
        // readNBytes is reliable across stream implementations; skip can short-read.
        byte[] discard = new byte[n];
        int read = in.readNBytes(discard, 0, n);
        if (read != n) {
            throw new EOFException("expected " + n + " padding bytes, got " + read);
        }
    }
}
