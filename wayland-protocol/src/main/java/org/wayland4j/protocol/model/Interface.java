package org.wayland4j.protocol.model;

import java.util.List;

public record Interface(
        String name,
        int version,
        String description,
        List<Request> requests,
        List<Event> events,
        List<EnumDef> enums
) {
    public boolean hasDestructor() {
        for (Request r : requests) if (r.destructor()) return true;
        return false;
    }

    public Request destructor() {
        for (Request r : requests) if (r.destructor()) return r;
        return null;
    }
}
