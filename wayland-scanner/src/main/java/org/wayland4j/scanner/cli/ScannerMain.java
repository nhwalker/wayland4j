package org.wayland4j.scanner.cli;

import org.wayland4j.scanner.codegen.JavaEmitter;
import org.wayland4j.scanner.model.Protocol;
import org.wayland4j.scanner.naming.Names;
import org.wayland4j.scanner.parser.Diagnostics;
import org.wayland4j.scanner.parser.ParseMode;
import org.wayland4j.scanner.parser.ProtocolParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI entry point.
 *
 * <pre>
 * wayland-scanner-java
 *   --input &lt;xml&gt;            (repeatable)
 *   --output-dir &lt;dir&gt;       (required)
 *   --base-package &lt;pkg&gt;     (optional; default org.wayland4j.protocol.&lt;name&gt;)
 *   --strict                 (default off)
 *   --help / -h
 * </pre>
 */
public final class ScannerMain {

    private static final String DEFAULT_BASE_PACKAGE = "org.wayland4j.protocol";

    public static void main(String[] args) throws Exception {
        Args parsed = Args.parse(args);
        if (parsed.help) {
            printUsage(System.out);
            return;
        }
        if (parsed.inputs.isEmpty() || parsed.outputDir == null) {
            printUsage(System.err);
            System.exit(2);
        }

        ProtocolParser parser = new ProtocolParser();
        JavaEmitter emitter = new JavaEmitter();
        ParseMode mode = parsed.strict ? ParseMode.STRICT : ParseMode.LENIENT;

        for (Path input : parsed.inputs) {
            Protocol protocol = parser.parse(input, new Diagnostics(mode));
            String pkg = parsed.basePackage != null
                    ? parsed.basePackage + "." + Names.packageSegment(protocol.name())
                    : DEFAULT_BASE_PACKAGE + "." + Names.packageSegment(protocol.name());
            emitter.emit(protocol, pkg, parsed.outputDir);
        }
    }

    private static void printUsage(java.io.PrintStream out) {
        out.println("""
                wayland-scanner-java --input <xml> [--input <xml> ...] --output-dir <dir> \
                [--base-package <pkg>] [--strict]
                """);
    }

    static final class Args {
        final List<Path> inputs = new ArrayList<>();
        Path outputDir;
        String basePackage;
        boolean strict;
        boolean help;

        static Args parse(String[] argv) {
            Args a = new Args();
            for (int i = 0; i < argv.length; i++) {
                String s = argv[i];
                switch (s) {
                    case "--input" -> a.inputs.add(Path.of(requireValue(argv, ++i, s)));
                    case "--output-dir" -> a.outputDir = Path.of(requireValue(argv, ++i, s));
                    case "--base-package" -> a.basePackage = requireValue(argv, ++i, s);
                    case "--strict" -> a.strict = true;
                    case "--help", "-h" -> a.help = true;
                    default -> throw new IllegalArgumentException("Unknown option: " + s);
                }
            }
            return a;
        }

        private static String requireValue(String[] argv, int index, String flag) {
            if (index >= argv.length) {
                throw new IllegalArgumentException(flag + " requires a value");
            }
            return argv[index];
        }
    }

    private ScannerMain() {}
}
