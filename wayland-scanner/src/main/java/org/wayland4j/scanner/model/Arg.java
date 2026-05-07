package org.wayland4j.scanner.model;

import java.util.Optional;

/**
 * One {@code <arg>} of a request or event.
 *
 * @param inlineNewIdInterface true when {@code type == NEW_ID} and no
 *     {@code interface=} attribute was present; this triggers the
 *     three-field expansion (string interfaceName, uint version, uint id) at
 *     codegen time. Only {@code wl_registry.bind} uses this in the core
 *     protocol.
 */
public record Arg(
        String name,
        ArgType type,
        String summary,
        Optional<String> interfaceName,
        boolean allowNull,
        Optional<EnumRef> enumRef,
        boolean inlineNewIdInterface) {

    public Arg {
        summary = summary == null ? "" : summary;
        interfaceName = interfaceName == null ? Optional.empty() : interfaceName;
        enumRef = enumRef == null ? Optional.empty() : enumRef;
    }
}
