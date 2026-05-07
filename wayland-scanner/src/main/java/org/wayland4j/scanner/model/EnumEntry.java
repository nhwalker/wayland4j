package org.wayland4j.scanner.model;

import java.util.OptionalInt;

public record EnumEntry(
        String name,
        long value,
        int since,
        OptionalInt deprecatedSince,
        String summary,
        Description description) {

    public EnumEntry {
        summary = summary == null ? "" : summary;
        description = description == null ? Description.EMPTY : description;
        deprecatedSince = deprecatedSince == null ? OptionalInt.empty() : deprecatedSince;
    }
}
