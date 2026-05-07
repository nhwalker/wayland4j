package org.wayland4j.scanner.model;

import java.util.List;

public record EnumDef(
        String name,
        boolean bitfield,
        int since,
        Description description,
        List<EnumEntry> entries) {

    public EnumDef {
        description = description == null ? Description.EMPTY : description;
        entries = List.copyOf(entries);
    }
}
