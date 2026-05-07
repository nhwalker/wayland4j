package org.wayland4j.scanner.codegen;

import org.wayland4j.scanner.model.Protocol;

/**
 * Per-protocol codegen state shared by emitter helpers. Holds the Java
 * package the protocol's interfaces are emitted into.
 */
public record CodegenContext(Protocol protocol, String packageName) {
}
