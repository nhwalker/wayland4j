package org.wayland4j.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.wayland4j.client.Display;
import org.wayland4j.client.EventQueue;
import org.wayland4j.client.protocol.WlCallback;

class EventQueueTest {

    @Test
    void dispatchQueueDeliversToProxiesAttachedToIt() throws Exception {
        assumeTrue(CompositorAvailable.check(), "no Wayland compositor: " + CompositorAvailable.describe());
        try (Display display = Display.connect();
             EventQueue queue = display.createEventQueue("wayland4j-test")) {

            WlCallback callback = display.sync();
            callback.setQueue(queue);

            AtomicInteger doneCount = new AtomicInteger();
            callback.setListener((self, callbackData) -> doneCount.incrementAndGet());

            // Driving roundtripQueue blocks until the callback's done event has
            // been dispatched on this queue.
            display.roundtripQueue(queue);

            assertEquals(1, doneCount.get(), "wl_callback.done should fire exactly once on the custom queue");
        }
    }
}
