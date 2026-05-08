package org.wayland4j.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.wayland4j.client.Display;
import org.wayland4j.client.protocol.WlCallback;

class SyncTest {

    @Test
    void syncCallbackFiresWithinRoundtrip() throws Exception {
        assumeTrue(CompositorAvailable.check(), "no Wayland compositor: " + CompositorAvailable.describe());
        try (Display display = Display.connect()) {
            AtomicInteger doneCount = new AtomicInteger();
            WlCallback callback = display.sync();
            callback.setListener((self, callbackData) -> doneCount.incrementAndGet());
            display.roundtrip();
            assertEquals(1, doneCount.get(), "wl_callback.done should fire exactly once");
        }
    }
}
