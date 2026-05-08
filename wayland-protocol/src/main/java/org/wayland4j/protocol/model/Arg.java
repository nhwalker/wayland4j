package org.wayland4j.protocol.model;

public record Arg(
        String name,
        ArgType type,
        String interfaceName,
        boolean nullable,
        String enumRef,
        String summary
) {
    public boolean nullableAllowed() {
        return type == ArgType.STRING || type == ArgType.OBJECT || type == ArgType.ARRAY;
    }
}
