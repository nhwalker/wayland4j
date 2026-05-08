package org.wayland4j.protocol.scanner;

import java.util.List;
import org.wayland4j.protocol.model.Arg;
import org.wayland4j.protocol.model.ArgType;

public final class SignatureEncoder {

    private SignatureEncoder() {
    }

    public static String forRequest(Integer since, List<Arg> args) {
        return encode(since, args, /*isEvent=*/false);
    }

    public static String forEvent(Integer since, List<Arg> args) {
        return encode(since, args, /*isEvent=*/true);
    }

    private static String encode(Integer since, List<Arg> args, boolean isEvent) {
        StringBuilder sb = new StringBuilder();
        if (since != null && since > 1) sb.append(since.intValue());
        for (Arg a : args) {
            // Special case: new_id without an explicit interface (only requests, e.g. wl_registry.bind)
            // expands on the wire to (string, uint, new_id).
            if (a.type() == ArgType.NEW_ID && a.interfaceName() == null && !isEvent) {
                sb.append("su");
            }
            if (a.nullable() && a.nullableAllowed()) sb.append('?');
            sb.append(a.type().signatureChar());
        }
        return sb.toString();
    }

    /** Number of wl_argument slots for the given args (after expansion of dynamic new_id). */
    public static int argCount(List<Arg> args, boolean isEvent) {
        int n = 0;
        for (Arg a : args) {
            if (a.type() == ArgType.NEW_ID && a.interfaceName() == null && !isEvent) {
                n += 3;
            } else {
                n += 1;
            }
        }
        return n;
    }
}
