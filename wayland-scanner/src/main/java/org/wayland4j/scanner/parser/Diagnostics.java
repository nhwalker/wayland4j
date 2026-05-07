package org.wayland4j.scanner.parser;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects parser warnings. In {@link ParseMode#LENIENT} mode warnings are
 * appended to {@link #warnings()} and echoed to {@code stderr}. In
 * {@link ParseMode#STRICT} mode the first warning is thrown as a
 * {@link ProtocolParseException}.
 */
public final class Diagnostics {

    private final ParseMode mode;
    private final PrintStream err;
    private final List<String> warnings = new ArrayList<>();

    public Diagnostics(ParseMode mode) {
        this(mode, System.err);
    }

    public Diagnostics(ParseMode mode, PrintStream err) {
        this.mode = mode;
        this.err = err;
    }

    public void warn(String source, int line, String fmt, Object... args) {
        String message = "%s:%d: %s".formatted(source, line, fmt.formatted(args));
        if (mode == ParseMode.STRICT) {
            throw new ProtocolParseException(message);
        }
        warnings.add(message);
        err.println("warning: " + message);
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }

    public ParseMode mode() {
        return mode;
    }
}
