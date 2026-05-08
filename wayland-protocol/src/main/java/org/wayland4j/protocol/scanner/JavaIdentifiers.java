package org.wayland4j.protocol.scanner;

import java.util.Set;

public final class JavaIdentifiers {

    private static final Set<String> RESERVED = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "final", "finally", "float", "for",
            "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "true", "false",
            "null", "yield", "record", "sealed", "permits", "var");

    private JavaIdentifiers() {
    }

    public static String typeName(String wlInterfaceName) {
        return upperCamel(wlInterfaceName);
    }

    public static String methodName(String wlMessageName) {
        return safeIdentifier(lowerCamel(wlMessageName));
    }

    public static String fieldName(String wlArgName) {
        return safeIdentifier(lowerCamel(wlArgName));
    }

    public static String constantName(String wlEntryName) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wlEntryName.length(); i++) {
            char c = wlEntryName.charAt(i);
            if (i == 0 && Character.isDigit(c)) sb.append('_');
            sb.append(Character.toUpperCase(c));
        }
        return safeConstant(sb.toString());
    }

    public static String upperCamel(String s) {
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_' || c == '-' || c == '.') {
                upper = true;
            } else if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String lowerCamel(String s) {
        String upper = upperCamel(s);
        if (upper.isEmpty()) return upper;
        return Character.toLowerCase(upper.charAt(0)) + upper.substring(1);
    }

    private static String safeIdentifier(String s) {
        if (RESERVED.contains(s)) return s + "_";
        return s;
    }

    private static String safeConstant(String s) {
        if (RESERVED.contains(s.toLowerCase())) return s + "_";
        return s;
    }
}
