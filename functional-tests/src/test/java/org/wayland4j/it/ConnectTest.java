package org.wayland4j.it;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.wayland4j.client.Display;

class ConnectTest {

    @Test
    void connectAndDisconnect() {
        assumeTrue(CompositorAvailable.check(), "no Wayland compositor: " + CompositorAvailable.describe());
        try (Display display = Display.connect()) {
            assertNotNull(display);
            assertTrue(display.fd() >= 0);
            assertTrue(display.id() == 1, "wl_display always has id=1");
        }
    }
}
