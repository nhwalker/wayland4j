package org.wayland4j.scanner.model;

import java.util.List;
import java.util.OptionalInt;

public record Event(
        String name,
        int opcode,
        int since,
        OptionalInt deprecatedSince,
        Description description,
        List<Arg> args) {

    public Event {
        description = description == null ? Description.EMPTY : description;
        deprecatedSince = deprecatedSince == null ? OptionalInt.empty() : deprecatedSince;
        args = List.copyOf(args);
    }
}
