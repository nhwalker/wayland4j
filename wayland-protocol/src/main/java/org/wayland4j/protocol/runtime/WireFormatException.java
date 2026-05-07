package org.wayland4j.protocol.runtime;

import java.io.IOException;

/**
 * Thrown when bytes on the Wayland wire violate the protocol: short reads,
 * impossible lengths, null where {@code allow-null} is not set, unknown opcode,
 * etc.
 */
public class WireFormatException extends IOException {

    public WireFormatException(String message) {
        super(message);
    }

    public WireFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
