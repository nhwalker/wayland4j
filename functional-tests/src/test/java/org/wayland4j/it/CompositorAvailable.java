package org.wayland4j.it;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper for the functional tests: returns {@code true} when a Wayland
 * compositor is reachable from this process. Tests use it via
 * {@code Assumptions.assumeTrue} so they skip cleanly on developer machines
 * without a session bus.
 */
final class CompositorAvailable {

    private CompositorAvailable() {
    }

    static boolean check() {
        String runtimeDir = System.getenv("XDG_RUNTIME_DIR");
        String displayName = System.getenv("WAYLAND_DISPLAY");
        if (runtimeDir == null || displayName == null) return false;
        Path socket = displayName.startsWith("/")
                ? Path.of(displayName)
                : Path.of(runtimeDir, displayName);
        return Files.exists(socket);
    }

    static String describe() {
        return "XDG_RUNTIME_DIR=" + System.getenv("XDG_RUNTIME_DIR")
                + " WAYLAND_DISPLAY=" + System.getenv("WAYLAND_DISPLAY");
    }
}
