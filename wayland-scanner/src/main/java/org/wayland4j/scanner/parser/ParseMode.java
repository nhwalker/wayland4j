package org.wayland4j.scanner.parser;

public enum ParseMode {
    /** Log warnings on unknown attributes/elements; keep going. */
    LENIENT,
    /** Promote any warning into a thrown {@link ProtocolParseException}. */
    STRICT
}
