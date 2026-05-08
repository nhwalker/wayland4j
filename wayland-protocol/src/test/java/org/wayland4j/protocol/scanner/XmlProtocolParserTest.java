package org.wayland4j.protocol.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.wayland4j.protocol.model.ArgType;
import org.wayland4j.protocol.model.Interface;
import org.wayland4j.protocol.model.Protocol;
import org.wayland4j.protocol.model.Request;

class XmlProtocolParserTest {

    private static Protocol load() {
        try (InputStream in = XmlProtocolParserTest.class.getClassLoader()
                .getResourceAsStream("org/wayland4j/protocol/wayland.xml")) {
            assertNotNull(in, "vendored wayland.xml is on the classpath");
            return XmlProtocolParser.parse(in);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void parsesCoreProtocolStructure() {
        Protocol p = load();
        assertEquals("wayland", p.name());
        assertFalse(p.interfaces().isEmpty());
        Interface display = findInterface(p, "wl_display");
        assertEquals(1, display.version());
        assertEquals(2, display.requests().size());
        assertEquals(2, display.events().size());
    }

    @Test
    void registryBindIsDynamicConstructor() {
        Protocol p = load();
        Interface registry = findInterface(p, "wl_registry");
        Request bind = registry.requests().getFirst();
        assertEquals("bind", bind.name());
        assertTrue(bind.isDynamicConstructor());
        // Two XML args (name + new_id-without-iface) on the wire become four
        // signature characters: u s u n.
        assertEquals(2, bind.args().size());
        assertEquals(ArgType.UINT, bind.args().get(0).type());
        assertEquals(ArgType.NEW_ID, bind.args().get(1).type());
    }

    @Test
    void destructorIsRecognised() {
        Protocol p = load();
        Interface shmPool = findInterface(p, "wl_shm_pool");
        assertTrue(shmPool.hasDestructor());
        Request destroy = shmPool.destructor();
        assertNotNull(destroy);
        assertEquals("destroy", destroy.name());
    }

    private static Interface findInterface(Protocol p, String name) {
        return p.interfaces().stream()
                .filter(i -> i.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing interface: " + name));
    }
}
