package org.wayland4j.scanner.model;

import java.util.Optional;

/**
 * A reference from an {@code <arg enum="...">} attribute to an enum definition.
 * The interface name is empty when the enum lives in the same interface as the
 * argument; otherwise it is the qualifier from {@code "iface.enum"}.
 */
public record EnumRef(Optional<String> interfaceName, String enumName) {

    public static EnumRef parse(String raw) {
        int dot = raw.indexOf('.');
        if (dot < 0) {
            return new EnumRef(Optional.empty(), raw);
        }
        return new EnumRef(Optional.of(raw.substring(0, dot)), raw.substring(dot + 1));
    }
}
