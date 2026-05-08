package org.wayland4j.protocol.model;

import java.util.List;

public record Protocol(
        String name,
        String copyright,
        String description,
        List<Interface> interfaces
) {
}
