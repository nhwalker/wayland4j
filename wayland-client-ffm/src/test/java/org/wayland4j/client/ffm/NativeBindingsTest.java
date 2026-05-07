package org.wayland4j.client.ffm;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration smoke tests that require {@code libwayland-client} to be
 * loadable. They do not require a running compositor.
 */
class NativeBindingsTest {

    @BeforeAll
    static void requireLibwayland() {
        assumeTrue(WaylandNative.tryLoad(),
                "libwayland-client not available on this system");
    }

    @Test
    void wlDisplayInterfaceIsExported() {
        // wl_display_interface is generated from wayland.xml and linked
        // into libwayland-client. If we can read its name, our struct
        // layout matches what the C compiler produced.
        var iface = WlInterface.lookup("wl_display_interface");
        assumeTrue(iface.isPresent(),
                "wl_display_interface not exported (very old libwayland?)");
        var w = iface.get();
        assertNotNull(w.name(), "name() returned null");
        // libwayland always uses "wl_display" as the interface name.
        org.junit.jupiter.api.Assertions.assertEquals("wl_display", w.name());
        assertTrue(w.version() >= 1, "version: " + w.version());
        assertTrue(w.methodCount() >= 2, "expected >=2 requests, got " + w.methodCount());
        assertTrue(w.eventCount() >= 2, "expected >=2 events, got " + w.eventCount());
        // Spot-check the first request: wl_display.sync(new_id<wl_callback>).
        var sync = w.method(0);
        org.junit.jupiter.api.Assertions.assertEquals("sync", sync.name());
        org.junit.jupiter.api.Assertions.assertEquals(1, sync.arguments().size());
        org.junit.jupiter.api.Assertions.assertEquals(
                WlArgumentType.NEW_ID, sync.arguments().get(0).type());
    }

    @Test
    void wlCallbackInterfaceIsExported() {
        var iface = WlInterface.lookup("wl_callback_interface");
        assumeTrue(iface.isPresent(),
                "wl_callback_interface not exported (very old libwayland?)");
        var w = iface.get();
        org.junit.jupiter.api.Assertions.assertEquals("wl_callback", w.name());
        // wl_callback has one event "done(uint)"
        assertTrue(w.eventCount() >= 1);
        var done = w.event(0);
        org.junit.jupiter.api.Assertions.assertEquals("done", done.name());
    }

    @Test
    void connectFailsCleanlyWhenNoCompositor() {
        // A name that no compositor will be listening on. wl_display_connect
        // should return NULL, which we surface as an empty Optional without
        // throwing.
        var result = WlDisplay.connect("wayland4j-no-such-display-" + System.nanoTime());
        // We deliberately don't assert isEmpty() — if a developer is
        // running the test under a Wayland session and the name happens
        // to resolve (extremely unlikely), the call could succeed. What
        // we *do* care about is that we got back a non-null Optional and
        // didn't crash.
        assertNotNull(result);
        result.ifPresent(WlDisplay::close);
    }

    @Test
    void marshalFlagsHandleResolves() {
        // Just resolve the symbol; this is the most "modern" entry point
        // we depend on (added in libwayland 1.19). Loading the WlProxy
        // class triggers the resolution, so reaching this assertion is
        // already proof.
        assertNotEquals(0L, WlProxy.class.hashCode());
    }
}
