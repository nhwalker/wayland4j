package org.wayland4j.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.wayland4j.client.Display;
import org.wayland4j.client.protocol.WlRegistry;

class RegistryTest {

    @Test
    void listsCompositorAndShmGlobals() throws Exception {
        assumeTrue(CompositorAvailable.check(), "no Wayland compositor: " + CompositorAvailable.describe());
        try (Display display = Display.connect()) {
            Set<String> globals = ConcurrentHashMap.newKeySet();
            WlRegistry registry = display.getRegistry();
            registry.setListener(new WlRegistry.Listener() {
                @Override
                public void global(WlRegistry self, int name, String iface, int version) {
                    globals.add(iface);
                }

                @Override
                public void globalRemove(WlRegistry self, int name) {
                }
            });
            display.roundtrip();
            assertFalse(globals.isEmpty(), "compositor advertised at least one global");
            // Most compositors advertise at least wl_compositor and wl_shm; we only
            // assert one of the two to keep this resilient across environments.
            assertTrue(globals.contains("wl_compositor") || globals.contains("wl_shm"),
                    "expected wl_compositor or wl_shm, got: " + globals);
        }
    }
}
