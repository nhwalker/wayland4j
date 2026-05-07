package org.wayland4j.scanner.naming;

import java.util.Set;

/**
 * Conversions between Wayland's snake_case identifiers and Java's
 * Pascal/camelCase, plus Java keyword escaping.
 */
public final class Names {

    private Names() {}

    /** All Java reserved words plus the contextual keywords currently treated as reserved in modern Java. */
    private static final Set<String> JAVA_RESERVED = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "false", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "null", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "true", "try", "void", "volatile", "while",
            "_", "record", "sealed", "permits", "yield", "var");

    /** Append {@code _} if {@code raw} collides with a Java reserved word. */
    public static String escape(String raw) {
        return JAVA_RESERVED.contains(raw) ? raw + "_" : raw;
    }

    /** {@code wl_surface} → {@code WlSurface}. */
    public static String pascal(String snake) {
        StringBuilder sb = new StringBuilder(snake.length());
        boolean upper = true;
        for (int i = 0; i < snake.length(); i++) {
            char c = snake.charAt(i);
            if (c == '_') {
                upper = true;
            } else {
                sb.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return sb.toString();
    }

    /** {@code attach_buffer} → {@code attachBuffer}, then keyword-escaped. */
    public static String camel(String snake) {
        String pascal = pascal(snake);
        if (pascal.isEmpty()) return pascal;
        return escape(Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1));
    }

    /** {@code my_flag} → {@code MY_FLAG}; leading digit prefixed with {@code _}. */
    public static String enumConstant(String name) {
        StringBuilder sb = new StringBuilder(name.length() + 2);
        if (!name.isEmpty() && Character.isDigit(name.charAt(0))) {
            sb.append('_');
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            sb.append(c == '-' ? '_' : Character.toUpperCase(c));
        }
        return escape(sb.toString());
    }

    /** Sanitises a protocol name into a Java package segment: lowercase, [a-z0-9_], leading-digit prefixed. */
    public static String packageSegment(String raw) {
        StringBuilder sb = new StringBuilder(raw.length() + 1);
        if (!raw.isEmpty() && Character.isDigit(raw.charAt(0))) {
            sb.append('_');
        }
        for (int i = 0; i < raw.length(); i++) {
            char c = Character.toLowerCase(raw.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        String result = sb.toString();
        return escape(result);
    }
}
