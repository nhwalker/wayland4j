package org.wayland4j.protocol.model;

import java.util.List;

public record Request(
        String name,
        int opcode,
        Integer since,
        boolean destructor,
        String description,
        List<Arg> args
) {
    public Arg newIdArg() {
        for (Arg a : args) if (a.type() == ArgType.NEW_ID) return a;
        return null;
    }

    public boolean isConstructor() {
        return newIdArg() != null;
    }

    public boolean isDynamicConstructor() {
        Arg n = newIdArg();
        return n != null && n.interfaceName() == null;
    }
}
