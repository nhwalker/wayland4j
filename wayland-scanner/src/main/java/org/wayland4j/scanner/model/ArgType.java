package org.wayland4j.scanner.model;

/** Wayland's eight wire-level argument types. */
public enum ArgType {
    INT,
    UINT,
    FIXED,
    STRING,
    OBJECT,
    NEW_ID,
    ARRAY,
    FD;

    public static ArgType fromXml(String xmlName) {
        return switch (xmlName) {
            case "int" -> INT;
            case "uint" -> UINT;
            case "fixed" -> FIXED;
            case "string" -> STRING;
            case "object" -> OBJECT;
            case "new_id" -> NEW_ID;
            case "array" -> ARRAY;
            case "fd" -> FD;
            default -> throw new IllegalArgumentException("Unknown arg type: " + xmlName);
        };
    }
}
