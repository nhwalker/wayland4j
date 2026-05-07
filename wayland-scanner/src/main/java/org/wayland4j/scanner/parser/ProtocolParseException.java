package org.wayland4j.scanner.parser;

public class ProtocolParseException extends RuntimeException {

    public ProtocolParseException(String message) {
        super(message);
    }

    public ProtocolParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
