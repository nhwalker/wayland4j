package org.wayland4j.client.ffm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

class WlArgumentTypeTest {

    @Test
    void emptySignature() {
        assertEquals(List.of(), WlArgumentType.parse(""));
        assertEquals(List.of(), WlArgumentType.parse(null));
    }

    @Test
    void parseSimpleSignature() {
        // wl_registry.global: "uobject_string_uint" → "usu"
        var parsed = WlArgumentType.parse("usu");
        assertEquals(3, parsed.size());
        assertEquals(WlArgumentType.UINT, parsed.get(0).type());
        assertEquals(WlArgumentType.STRING, parsed.get(1).type());
        assertEquals(WlArgumentType.UINT, parsed.get(2).type());
        for (var e : parsed) {
            assertFalse(e.nullable(), "no '?' markers");
        }
    }

    @Test
    void leadingSinceVersionStripped() {
        // wl_seat events from wayland.xml use a leading version digit.
        var parsed = WlArgumentType.parse("2u");
        assertEquals(1, parsed.size());
        assertEquals(WlArgumentType.UINT, parsed.get(0).type());
    }

    @Test
    void nullableMarkerSetsFlag() {
        // wl_registry.bind: "usun" with nullable interface? Actually
        // wl_data_device.set_selection: "?on" — first arg may be null.
        var parsed = WlArgumentType.parse("?on");
        assertEquals(2, parsed.size());
        assertEquals(WlArgumentType.OBJECT, parsed.get(0).type());
        assertTrue(parsed.get(0).nullable());
        assertEquals(WlArgumentType.NEW_ID, parsed.get(1).type());
        assertFalse(parsed.get(1).nullable());
    }

    @Test
    void allArgumentTypesCoverWayland() {
        var parsed = WlArgumentType.parse("iuf?son?ah");
        assertEquals(8, parsed.size());
        assertEquals(WlArgumentType.INT, parsed.get(0).type());
        assertEquals(WlArgumentType.UINT, parsed.get(1).type());
        assertEquals(WlArgumentType.FIXED, parsed.get(2).type());
        assertEquals(WlArgumentType.STRING, parsed.get(3).type());
        assertTrue(parsed.get(3).nullable());
        assertEquals(WlArgumentType.OBJECT, parsed.get(4).type());
        assertEquals(WlArgumentType.NEW_ID, parsed.get(5).type());
        assertEquals(WlArgumentType.ARRAY, parsed.get(6).type());
        assertTrue(parsed.get(6).nullable());
        assertEquals(WlArgumentType.FD, parsed.get(7).type());
    }

    @Test
    void unknownCharThrows() {
        assertThrows(WaylandClientException.class, () -> WlArgumentType.parse("xy"));
    }

    @Test
    void roundTripSymbolLookup() {
        for (var t : WlArgumentType.values()) {
            assertEquals(t, WlArgumentType.of(t.symbol));
        }
    }
}
