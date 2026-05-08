package org.wayland4j.protocol.model;

import java.util.List;

public record Event(
        String name,
        int opcode,
        Integer since,
        String description,
        List<Arg> args
) {
}
