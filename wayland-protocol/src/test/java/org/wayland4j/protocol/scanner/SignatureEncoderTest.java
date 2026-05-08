package org.wayland4j.protocol.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.wayland4j.protocol.model.Arg;
import org.wayland4j.protocol.model.ArgType;

class SignatureEncoderTest {

    @Test
    void encodesPrimitiveArgs() {
        List<Arg> args = List.of(
                new Arg("a", ArgType.INT, null, false, null, null),
                new Arg("b", ArgType.UINT, null, false, null, null),
                new Arg("c", ArgType.STRING, null, false, null, null));
        assertEquals("ius", SignatureEncoder.forRequest(null, args));
    }

    @Test
    void includesSinceVersionPrefixForVersionAboveOne() {
        List<Arg> args = List.of(new Arg("x", ArgType.INT, null, false, null, null));
        assertEquals("3i", SignatureEncoder.forRequest(3, args));
    }

    @Test
    void omitsSinceForVersionOne() {
        List<Arg> args = List.of(new Arg("x", ArgType.INT, null, false, null, null));
        assertEquals("i", SignatureEncoder.forRequest(1, args));
    }

    @Test
    void encodesNullableObjectAndString() {
        List<Arg> args = List.of(
                new Arg("buf", ArgType.OBJECT, "wl_buffer", true, null, null),
                new Arg("title", ArgType.STRING, null, true, null, null));
        assertEquals("?o?s", SignatureEncoder.forRequest(null, args));
    }

    @Test
    void expandsDynamicNewIdToSun() {
        // wl_registry.bind: (uint name, new_id id [no iface]) → "usun" on the wire.
        List<Arg> args = List.of(
                new Arg("name", ArgType.UINT, null, false, null, null),
                new Arg("id", ArgType.NEW_ID, null, false, null, null));
        assertEquals("usun", SignatureEncoder.forRequest(null, args));
    }

    @Test
    void eventsDoNotExpandDynamicNewId() {
        // Events never carry the dynamic new_id form; the same arg shape, in event
        // position, would just emit "n".
        List<Arg> args = List.of(new Arg("id", ArgType.NEW_ID, "wl_data_offer", false, null, null));
        assertEquals("n", SignatureEncoder.forEvent(null, args));
    }
}
