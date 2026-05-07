package org.wayland4j.scanner.model;

import java.util.List;
import java.util.OptionalInt;

public record Request(
        String name,
        int opcode,
        int since,
        OptionalInt deprecatedSince,
        boolean destructor,
        Description description,
        List<Arg> args) {

    public Request {
        description = description == null ? Description.EMPTY : description;
        deprecatedSince = deprecatedSince == null ? OptionalInt.empty() : deprecatedSince;
        args = List.copyOf(args);
    }
}
