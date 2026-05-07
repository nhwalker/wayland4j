package org.wayland4j.scanner.parser;

import org.junit.jupiter.api.Test;
import org.wayland4j.scanner.model.ArgType;
import org.wayland4j.scanner.model.EnumDef;
import org.wayland4j.scanner.model.Interface;
import org.wayland4j.scanner.model.Protocol;
import org.wayland4j.scanner.model.Request;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolParserTest {

    private static final String SAMPLE = """
        <?xml version="1.0" encoding="UTF-8"?>
        <protocol name="example">
          <copyright>(c) example</copyright>
          <interface name="ex_thing" version="3">
            <description summary="A thing">
              Long body of text describing the thing.
            </description>
            <request name="set" since="2">
              <description summary="set a value"/>
              <arg name="value" type="int" summary="a value"/>
              <arg name="flags" type="uint" enum="ex_thing.flag" allow-null="false"/>
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
            <enum name="error" since="1">
              <description summary="errors"/>
              <entry name="bad" value="0" summary="bad"/>
              <entry name="2_part" value="1"/>
            </enum>
            <enum name="flag" bitfield="true">
              <entry name="alpha" value="0x1"/>
              <entry name="beta" value="0x2" deprecated-since="3"/>
            </enum>
          </interface>
        </protocol>
        """;

    @Test
    void parsesAllConstructs() {
        Protocol p = new ProtocolParser().parse(SAMPLE, "<inline>", new Diagnostics(ParseMode.LENIENT));
        assertEquals("example", p.name());
        assertTrue(p.copyright().contains("example"));

        Interface iface = p.interfaces().get(0);
        assertEquals("ex_thing", iface.name());
        assertEquals(3, iface.version());
        assertEquals("A thing", iface.description().summary());
        assertTrue(iface.description().body().startsWith("Long body"));

        // requests assigned opcodes by document order
        Request set = iface.requests().get(0);
        assertEquals("set", set.name());
        assertEquals(0, set.opcode());
        assertEquals(2, set.since());
        assertEquals(2, set.args().size());
        assertEquals(ArgType.INT, set.args().get(0).type());
        assertEquals("ex_thing", set.args().get(1).enumRef().orElseThrow().interfaceName().orElseThrow());

        Request bind = iface.requests().get(1);
        assertEquals(1, bind.opcode());
        assertTrue(bind.args().get(1).inlineNewIdInterface());

        Request destroy = iface.requests().get(2);
        assertEquals(2, destroy.opcode());
        assertTrue(destroy.destructor());
        assertEquals(0, destroy.args().size());

        var event = iface.events().get(0);
        assertEquals("changed", event.name());
        assertEquals(0, event.opcode());
        assertEquals(3, event.deprecatedSince().getAsInt());
        assertEquals(ArgType.FIXED, event.args().get(4).type());
        assertTrue(event.args().get(0).allowNull());

        EnumDef errors = iface.enums().get(0);
        assertEquals("error", errors.name());
        assertFalse(errors.bitfield());
        assertEquals(2, errors.entries().size());
        assertEquals("2_part", errors.entries().get(1).name());

        EnumDef flags = iface.enums().get(1);
        assertTrue(flags.bitfield());
        assertEquals(1, flags.entries().get(0).value());
        assertEquals(2, flags.entries().get(1).value());
        assertEquals(3, flags.entries().get(1).deprecatedSince().getAsInt());
    }

    @Test
    void lenientLogsUnknownAttribute() {
        var stderr = new ByteArrayOutputStream();
        var diag = new Diagnostics(ParseMode.LENIENT, new PrintStream(stderr, true, StandardCharsets.UTF_8));
        String xml = """
            <protocol name="ex">
              <interface name="i" version="1" mystery="yes"/>
            </protocol>
            """;
        new ProtocolParser().parse(xml, "<inline>", diag);
        assertEquals(1, diag.warnings().size());
        assertTrue(diag.warnings().get(0).contains("unknown attribute mystery"));
    }

    @Test
    void strictRejectsUnknownAttribute() {
        var diag = new Diagnostics(ParseMode.STRICT);
        String xml = """
            <protocol name="ex">
              <interface name="i" version="1" mystery="yes"/>
            </protocol>
            """;
        assertThrows(ProtocolParseException.class,
                () -> new ProtocolParser().parse(xml, "<inline>", diag));
    }
}
