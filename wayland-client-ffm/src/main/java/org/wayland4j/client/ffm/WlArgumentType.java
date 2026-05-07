package org.wayland4j.client.ffm;

import java.util.List;

/**
 * The eight Wayland argument kinds. Each is identified by a single
 * character in a {@code wl_message} signature; see the
 * <a href="https://wayland-book.com/protocol-design/wire-protocol.html">wire
 * protocol chapter</a> for the on-wire encodings.
 */
public enum WlArgumentType {

    /** {@code 'i'} — signed 32-bit int. */
    INT('i'),
    /** {@code 'u'} — unsigned 32-bit int. */
    UINT('u'),
    /** {@code 'f'} — {@code wl_fixed_t} (24.8 fixed point). */
    FIXED('f'),
    /** {@code 's'} — NUL-terminated UTF-8 string. */
    STRING('s'),
    /** {@code 'o'} — object id (proxy). */
    OBJECT('o'),
    /** {@code 'n'} — new object id. */
    NEW_ID('n'),
    /** {@code 'a'} — {@code wl_array} payload. */
    ARRAY('a'),
    /** {@code 'h'} — file descriptor (passed out of band over the socket). */
    FD('h');

    public final char symbol;

    WlArgumentType(char symbol) {
        this.symbol = symbol;
    }

    /** Resolve a signature character. Throws on unknown characters. */
    public static WlArgumentType of(char c) {
        for (WlArgumentType t : values()) {
            if (t.symbol == c) {
                return t;
            }
        }
        throw new WaylandClientException(
                "Unknown wl_message signature character: '" + c + "'");
    }

    /**
     * One parsed signature element.
     *
     * @param type     the argument kind
     * @param nullable true if the argument was prefixed with {@code '?'}
     */
    public record Element(WlArgumentType type, boolean nullable) {}

    /**
     * Parse a {@code wl_message} signature into a list of elements.
     *
     * <p>The signature optionally starts with one or more decimal digits
     * encoding the {@code since} version of the request/event; those digits
     * are skipped. The remaining characters are the per-argument kinds,
     * each optionally preceded by {@code '?'} to mark the argument as
     * nullable.
     */
    public static List<Element> parse(String signature) {
        if (signature == null) {
            return List.of();
        }
        int i = 0;
        // Skip the leading "since" version digits.
        while (i < signature.length() && Character.isDigit(signature.charAt(i))) {
            i++;
        }
        List<Element> out = new java.util.ArrayList<>();
        boolean nullable = false;
        for (; i < signature.length(); i++) {
            char c = signature.charAt(i);
            if (c == '?') {
                nullable = true;
                continue;
            }
            out.add(new Element(of(c), nullable));
            nullable = false;
        }
        return List.copyOf(out);
    }
}
