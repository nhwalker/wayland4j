package org.wayland4j.protocol.model;

public enum ArgType {
    INT('i'),
    UINT('u'),
    FIXED('f'),
    STRING('s'),
    OBJECT('o'),
    NEW_ID('n'),
    ARRAY('a'),
    FD('h');

    private final char signatureChar;

    ArgType(char c) {
        this.signatureChar = c;
    }

    public char signatureChar() {
        return signatureChar;
    }

    public static ArgType fromXml(String type) {
        return switch (type) {
            case "int" -> INT;
            case "uint" -> UINT;
            case "fixed" -> FIXED;
            case "string" -> STRING;
            case "object" -> OBJECT;
            case "new_id" -> NEW_ID;
            case "array" -> ARRAY;
            case "fd" -> FD;
            default -> throw new IllegalArgumentException("unknown arg type: " + type);
        };
    }
}
