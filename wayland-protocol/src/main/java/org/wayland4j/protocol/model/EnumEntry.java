package org.wayland4j.protocol.model;

public record EnumEntry(
        String name,
        long value,
        Integer since,
        String summary
) {
}
