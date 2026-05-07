package org.wayland4j.scanner.codegen;

import org.junit.jupiter.api.Test;
import org.wayland4j.scanner.model.Protocol;
import org.wayland4j.scanner.parser.Diagnostics;
import org.wayland4j.scanner.parser.ParseMode;
import org.wayland4j.scanner.parser.ProtocolParser;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaEmitterTest {

    private static final String SAMPLE = """
        <?xml version="1.0" encoding="UTF-8"?>
        <protocol name="example">
          <copyright>(c) example</copyright>
          <interface name="ex_thing" version="3">
            <description summary="A thing">Long body.</description>
            <request name="set" since="2">
              <description summary="set"/>
              <arg name="value" type="int"/>
              <arg name="flags" type="uint" enum="ex_thing.flag"/>
              <arg name="class" type="string"/>
            </request>
            <request name="bind">
              <arg name="name" type="uint"/>
              <arg name="id" type="new_id"/>
            </request>
            <request name="destroy" type="destructor"/>
            <event name="changed" deprecated-since="3">
              <arg name="who" type="object" interface="ex_other" allow-null="true"/>
              <arg name="payload" type="array"/>
              <arg name="text" type="string" allow-null="true"/>
              <arg name="fd" type="fd"/>
              <arg name="ratio" type="fixed"/>
            </event>
            <enum name="error">
              <entry name="bad" value="0"/>
              <entry name="2_part" value="1"/>
            </enum>
            <enum name="flag" bitfield="true">
              <entry name="alpha" value="0x1"/>
              <entry name="beta" value="0x2"/>
            </enum>
          </interface>
          <interface name="ex_other" version="1">
            <description summary="Other"/>
            <event name="ping"><arg name="serial" type="uint"/></event>
          </interface>
          <interface name="ex_empty" version="1">
            <description summary="No requests, no events"/>
          </interface>
        </protocol>
        """;

    @Test
    void compilesEmittedSources() throws Exception {
        Path out = Files.createTempDirectory("wayland-emitter-test");
        Protocol protocol = new ProtocolParser().parse(SAMPLE, "<inline>", new Diagnostics(ParseMode.LENIENT));
        new JavaEmitter().emit(protocol, "org.wayland4j.test.example", out);

        List<JavaFileObject> sources = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(out)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> sources.add(new InMemoryJavaSource(p, readString(p))));
        }
        assertTrue(sources.size() >= 3, "expected one .java per interface, got " + sources.size());

        Path classOut = Files.createTempDirectory("wayland-emitter-classes");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new javax.tools.DiagnosticCollector<JavaFileObject>();
        var fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        var task = compiler.getTask(null, fileManager, diagnostics,
                List.of("-proc:none", "--release", "21", "-d", classOut.toString()),
                null, sources);
        boolean ok = task.call();

        if (!ok) {
            StringBuilder report = new StringBuilder("compilation failed:\n");
            diagnostics.getDiagnostics().forEach(d -> report.append(d).append("\n"));
            for (JavaFileObject s : sources) {
                report.append("--- ").append(s.getName()).append(" ---\n")
                        .append(s.getCharContent(false)).append("\n");
            }
            throw new AssertionError(report.toString());
        }
        assertTrue(ok);
    }

    @Test
    void destructorAndKeywordEscapeRendered() {
        Protocol protocol = new ProtocolParser().parse(SAMPLE, "<inline>", new Diagnostics(ParseMode.LENIENT));
        String src = new JavaEmitter().renderInterfaceFile(protocol, "org.wayland4j.test.example",
                protocol.interfaces().get(0));
        assertTrue(src.contains("DESTRUCTOR = true"), "destructor marker missing");
        assertTrue(src.contains("class_"), "Java keyword arg should be escaped");
        assertTrue(src.contains("public static final int ALPHA"), "bitfield entry constant missing");
        assertTrue(src.contains("public enum Error"), "non-bitfield enum missing");
        assertTrue(src.contains("_2_PART"), "leading-digit enum entry should be prefixed");
        assertTrue(src.contains("interfaceName"), "inline new_id expansion missing");
    }

    @Test
    void emptyInterfaceEmitsFallbackDispatcher() {
        Protocol protocol = new ProtocolParser().parse(SAMPLE, "<inline>", new Diagnostics(ParseMode.LENIENT));
        var empty = protocol.interfaces().get(2);
        assertEquals("ex_empty", empty.name());
        String src = new JavaEmitter().renderInterfaceFile(protocol, "org.wayland4j.test.example", empty);
        assertTrue(src.contains("has no request messages") || src.contains("has no event messages"));
    }

    private static String readString(Path p) {
        try { return Files.readString(p); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private static final class InMemoryJavaSource extends SimpleJavaFileObject {
        private final String source;
        InMemoryJavaSource(Path file, String source) {
            super(URI.create("mem:///" + file.getFileName()), Kind.SOURCE);
            this.source = source;
        }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return source; }
    }
}
