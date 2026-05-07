package org.wayland4j.scanner.model;

import java.util.List;

public record Protocol(
        String name,
        String copyright,
        Description description,
        List<Interface> interfaces) {

    public Protocol {
        copyright = copyright == null ? "" : copyright;
        description = description == null ? Description.EMPTY : description;
        interfaces = List.copyOf(interfaces);
    }
}
