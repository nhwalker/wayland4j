package org.wayland4j.protocol.integration;

import org.junit.jupiter.api.Test;
import org.wayland4j.protocol.runtime.FdSink;
import org.wayland4j.protocol.runtime.FdSource;
import org.wayland4j.protocol.runtime.WireIO;
import org.wayland4j.protocol.wayland.WlRegistry;
import org.wayland4j.protocol.wayland.WlShm;
import org.wayland4j.protocol.wayland.WlSurface;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WireRoundTripTest {

    private static final FdSink REJECTING_SINK = fd -> { throw new AssertionError("no fds expected"); };
    private static final FdSource REJECTING_SOURCE = () -> { throw new AssertionError("no fds expected"); };

    @Test
    void surfaceAttachRoundTrip() throws Exception {
        var original = new WlSurface.Request.Attach(42, 10, 20);
        var bytes = new ByteArrayOutputStream();
        original.writeTo(bytes, REJECTING_SINK);

        var read = WlSurface.Request.readPayload(
                new ByteArrayInputStream(bytes.toByteArray()),
                WlSurface.Request.Attach.OPCODE,
                REJECTING_SOURCE);

        assertEquals(original, read);
        // 3 ints, no header => 12 bytes
        assertEquals(12, bytes.size());
    }

    @Test
    void shmCreatePoolMovesFdViaSideChannel() throws Exception {
        Deque<Integer> wireQueue = new ArrayDeque<>();
        FdSink sink = wireQueue::push;
        FdSource source = wireQueue::pop;

        var original = new WlShm.Request.CreatePool(/*id=*/77, /*fd=*/5, /*size=*/4096);
        var bytes = new ByteArrayOutputStream();
        original.writeTo(bytes, sink);

        // fd contributes ZERO bytes to the wire; only id (uint) + size (int) on the wire.
        assertEquals(8, bytes.size(), "fd args must not appear in the byte stream");
        assertEquals(1, wireQueue.size());
        assertEquals(5, wireQueue.peek());

        var read = (WlShm.Request.CreatePool) WlShm.Request.readPayload(
                new ByteArrayInputStream(bytes.toByteArray()),
                WlShm.Request.CreatePool.OPCODE,
                source);
        assertEquals(original, read);
        assertTrue(wireQueue.isEmpty());
    }

    @Test
    void registryBindHandWrittenBytes() throws Exception {
        var bind = new WlRegistry.Request.Bind(/*name=*/1, /*interfaceName=*/"wl_compositor", /*version=*/4, /*id=*/2);
        var out = new ByteArrayOutputStream();
        bind.writeTo(out, REJECTING_SINK);

        // Hand-computed expected bytes:
        // name (uint LE) = 1                     -> 01 00 00 00
        // string length = "wl_compositor".length+1 = 14 -> 0e 00 00 00
        // body = w l _ c o m p o s i t o r \0    -> 14 chars + NUL = 14 bytes
        // pad to 4: 14 % 4 == 2 -> 2 pad bytes
        // version = 4                            -> 04 00 00 00
        // id      = 2                            -> 02 00 00 00
        byte[] expected = {
                0x01, 0x00, 0x00, 0x00,
                0x0e, 0x00, 0x00, 0x00,
                'w', 'l', '_', 'c', 'o', 'm', 'p', 'o', 's', 'i', 't', 'o', 'r', 0x00,
                0x00, 0x00,
                0x04, 0x00, 0x00, 0x00,
                0x02, 0x00, 0x00, 0x00,
        };
        assertArrayEquals(expected, out.toByteArray());

        var read = WlRegistry.Request.readPayload(
                new ByteArrayInputStream(out.toByteArray()),
                WlRegistry.Request.Bind.OPCODE,
                REJECTING_SOURCE);
        assertInstanceOf(WlRegistry.Request.Bind.class, read);
        assertEquals(bind, read);
    }

    @Test
    void framedHeaderRoundTrip() throws Exception {
        var attach = new WlSurface.Request.Attach(99, 1, 2);
        var payload = new ByteArrayOutputStream();
        attach.writeTo(payload, REJECTING_SINK);
        byte[] body = payload.toByteArray();

        int objectId = 7;
        int size = body.length + 8;
        var framed = new ByteArrayOutputStream();
        WireIO.writeUInt(framed, objectId);
        WireIO.writeUInt(framed, WireIO.packSizeOpcode(size, attach.opcode()));
        framed.write(body);

        var in = new ByteArrayInputStream(framed.toByteArray());
        int readObjectId = WireIO.readUInt(in);
        int sizeOpcode = WireIO.readUInt(in);
        assertEquals(objectId, readObjectId);
        assertEquals(size, WireIO.headerSize(sizeOpcode));
        assertEquals(attach.opcode(), WireIO.headerOpcode(sizeOpcode));

        var read = WlSurface.Request.readPayload(in, WireIO.headerOpcode(sizeOpcode), REJECTING_SOURCE);
        assertEquals(attach, read);
    }
}
