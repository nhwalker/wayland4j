package org.wayland4j.protocol.model;

import java.util.List;

public record EnumDef(
        String name,
        Integer since,
        boolean bitfield,
        String description,
        List<EnumEntry> entries
) {
}
