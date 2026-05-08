package org.wayland4j.it;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.wayland4j.client.Display;
import org.wayland4j.client.protocol.WlCompositor;
import org.wayland4j.client.protocol.WlRegistry;
import org.wayland4j.client.protocol.WlSurface;
import org.wayland4j.client.xdgshell.XdgSurface;
import org.wayland4j.client.xdgshell.XdgToplevel;
import org.wayland4j.client.xdgshell.XdgWmBase;

class XdgShellTest {

    @Test
    void roundTripsAConfigure() throws Exception {
        assumeTrue(CompositorAvailable.check(), "no Wayland compositor: " + CompositorAvailable.describe());
        try (Display display = Display.connect()) {
            AtomicReference<WlCompositor> compositorRef = new AtomicReference<>();
            AtomicReference<XdgWmBase> xdgRef = new AtomicReference<>();

            WlRegistry registry = display.getRegistry();
            registry.setListener(new WlRegistry.Listener() {
                @Override
                public void global(WlRegistry self, int name, String iface, int version) {
                    if (WlCompositor.INTERFACE_NAME.equals(iface)) {
                        compositorRef.set(self.bind(name, WlCompositor.class, Math.min(version, 6)));
                    } else if (XdgWmBase.INTERFACE_NAME.equals(iface)) {
                        xdgRef.set(self.bind(name, XdgWmBase.class, Math.min(version, 6)));
                    }
                }

                @Override
                public void globalRemove(WlRegistry self, int name) {
                }
            });
            display.roundtrip();

            WlCompositor compositor = compositorRef.get();
            XdgWmBase xdg = xdgRef.get();
            assumeTrue(compositor != null && xdg != null,
                    "compositor lacks wl_compositor or xdg_wm_base");

            xdg.setListener((self, serial) -> self.pong(serial));

            try (WlSurface surface = compositor.createSurface();
                 XdgSurface xdgSurface = xdg.getXdgSurface(surface);
                 XdgToplevel toplevel = xdgSurface.getToplevel()) {

                AtomicInteger configures = new AtomicInteger();
                xdgSurface.setListener((self, serial) -> {
                    configures.incrementAndGet();
                    self.ackConfigure(serial);
                });
                toplevel.setListener(new XdgToplevel.Listener() {
                    @Override public void configure(XdgToplevel self, int width, int height, byte[] states) {}
                    @Override public void close(XdgToplevel self) {}
                    @Override public void configureBounds(XdgToplevel self, int width, int height) {}
                    @Override public void wmCapabilities(XdgToplevel self, byte[] capabilities) {}
                });

                toplevel.setTitle("wayland4j XdgShellTest");
                surface.commit();
                // Two roundtrips: first commit triggers compositor's configure, second
                // ensures we receive it.
                display.roundtrip();
                display.roundtrip();

                assertNotNull(xdgSurface, "xdg_surface should have been created");
                assertTrue(configures.get() >= 1,
                        "expected at least one xdg_surface.configure event, got " + configures.get());
            } finally {
                xdg.close();
            }
        }
    }
}
