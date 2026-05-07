package org.wayland4j.scanner.model;

import java.util.List;

public record Interface(
        String name,
        int version,
        Description description,
        List<Request> requests,
        List<Event> events,
        List<EnumDef> enums) {

    public Interface {
        description = description == null ? Description.EMPTY : description;
        requests = List.copyOf(requests);
        events = List.copyOf(events);
        enums = List.copyOf(enums);
    }
}
