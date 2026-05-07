package org.wayland4j.scanner.parser;

import org.wayland4j.scanner.model.Arg;
import org.wayland4j.scanner.model.ArgType;
import org.wayland4j.scanner.model.Description;
import org.wayland4j.scanner.model.EnumDef;
import org.wayland4j.scanner.model.EnumEntry;
import org.wayland4j.scanner.model.EnumRef;
import org.wayland4j.scanner.model.Event;
import org.wayland4j.scanner.model.Interface;
import org.wayland4j.scanner.model.Protocol;
import org.wayland4j.scanner.model.Request;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * StAX-based parser for Wayland protocol XML. Lenient by default (logs
 * warnings on unknown attributes/elements via {@link Diagnostics}); strict
 * mode promotes warnings to thrown {@link ProtocolParseException}s.
 */
public final class ProtocolParser {

    private static final Set<String> PROTOCOL_ATTRS = Set.of("name");
    private static final Set<String> INTERFACE_ATTRS = Set.of("name", "version", "frozen");
    private static final Set<String> REQUEST_ATTRS = Set.of("name", "type", "since", "deprecated-since");
    private static final Set<String> EVENT_ATTRS = Set.of("name", "type", "since", "deprecated-since");
    private static final Set<String> ARG_ATTRS = Set.of(
            "name", "type", "summary", "interface", "allow-null", "enum");
    private static final Set<String> ENUM_ATTRS = Set.of("name", "bitfield", "since");
    private static final Set<String> ENTRY_ATTRS = Set.of("name", "value", "summary", "since", "deprecated-since");
    private static final Set<String> DESCRIPTION_ATTRS = Set.of("summary");

    public Protocol parse(Path file, ParseMode mode) {
        try (InputStream in = Files.newInputStream(file)) {
            return parse(in, file.toString(), new Diagnostics(mode));
        } catch (Exception e) {
            throw new ProtocolParseException("failed to read " + file, e);
        }
    }

    public Protocol parse(Path file, Diagnostics diag) {
        try (InputStream in = Files.newInputStream(file)) {
            return parse(in, file.toString(), diag);
        } catch (Exception e) {
            throw new ProtocolParseException("failed to read " + file, e);
        }
    }

    public Protocol parse(String xml, String source, Diagnostics diag) {
        return parse(new StringReader(xml), source, diag);
    }

    public Protocol parse(InputStream in, String source, Diagnostics diag) {
        try {
            XMLStreamReader reader = newFactory().createXMLStreamReader(source, in);
            return parseProtocol(reader, source, diag);
        } catch (XMLStreamException e) {
            throw new ProtocolParseException("XML error in " + source + ": " + e.getMessage(), e);
        }
    }

    public Protocol parse(Reader xml, String source, Diagnostics diag) {
        try {
            XMLStreamReader reader = newFactory().createXMLStreamReader(source, xml);
            return parseProtocol(reader, source, diag);
        } catch (XMLStreamException e) {
            throw new ProtocolParseException("XML error in " + source + ": " + e.getMessage(), e);
        }
    }

    private static XMLInputFactory newFactory() {
        XMLInputFactory f = XMLInputFactory.newDefaultFactory();
        f.setProperty(XMLInputFactory.IS_COALESCING, true);
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return f;
    }

    // ---- protocol ---------------------------------------------------------

    private Protocol parseProtocol(XMLStreamReader r, String source, Diagnostics diag) throws XMLStreamException {
        advanceToStartElement(r);
        requireElement(r, "protocol");
        checkAttrs(r, PROTOCOL_ATTRS, source, diag);

        String name = required(r, "name", source, diag);
        String copyright = "";
        Description description = Description.EMPTY;
        List<Interface> interfaces = new ArrayList<>();

        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals("protocol")) break;
            if (ev != XMLStreamConstants.START_ELEMENT) continue;
            switch (r.getLocalName()) {
                case "copyright" -> copyright = readElementText(r).trim();
                case "description" -> description = parseDescription(r, source, diag);
                case "interface" -> interfaces.add(parseInterface(r, source, diag));
                default -> {
                    diag.warn(source, line(r), "unknown element <%s> inside <protocol>", r.getLocalName());
                    skipElement(r);
                }
            }
        }
        return new Protocol(name, copyright, description, interfaces);
    }

    // ---- interface --------------------------------------------------------

    private Interface parseInterface(XMLStreamReader r, String source, Diagnostics diag) throws XMLStreamException {
        checkAttrs(r, INTERFACE_ATTRS, source, diag);
        String name = required(r, "name", source, diag);
        int version = parseIntAttr(r, "version", 1, source, diag);

        Description description = Description.EMPTY;
        List<Request> requests = new ArrayList<>();
        List<Event> events = new ArrayList<>();
        List<EnumDef> enums = new ArrayList<>();
        int reqOpcode = 0;
        int evOpcode = 0;

        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals("interface")) break;
            if (ev != XMLStreamConstants.START_ELEMENT) continue;
            switch (r.getLocalName()) {
                case "description" -> description = parseDescription(r, source, diag);
                case "request" -> requests.add(parseRequest(r, reqOpcode++, source, diag));
                case "event" -> events.add(parseEvent(r, evOpcode++, source, diag));
                case "enum" -> enums.add(parseEnum(r, source, diag));
                default -> {
                    diag.warn(source, line(r), "unknown element <%s> inside <interface>", r.getLocalName());
                    skipElement(r);
                }
            }
        }
        return new Interface(name, version, description, requests, events, enums);
    }

    // ---- request / event --------------------------------------------------

    private Request parseRequest(XMLStreamReader r, int opcode, String source, Diagnostics diag) throws XMLStreamException {
        checkAttrs(r, REQUEST_ATTRS, source, diag);
        String name = required(r, "name", source, diag);
        int since = parseIntAttr(r, "since", 1, source, diag);
        OptionalInt deprecatedSince = parseOptionalIntAttr(r, "deprecated-since", source, diag);
        String type = attr(r, "type");
        boolean destructor = "destructor".equals(type);
        if (type != null && !destructor) {
            diag.warn(source, line(r), "unknown request type=\"%s\"", type);
        }

        Description description = Description.EMPTY;
        List<Arg> args = new ArrayList<>();
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals("request")) break;
            if (ev != XMLStreamConstants.START_ELEMENT) continue;
            switch (r.getLocalName()) {
                case "description" -> description = parseDescription(r, source, diag);
                case "arg" -> args.add(parseArg(r, source, diag));
                default -> {
                    diag.warn(source, line(r), "unknown element <%s> inside <request>", r.getLocalName());
                    skipElement(r);
                }
            }
        }
        return new Request(name, opcode, since, deprecatedSince, destructor, description, args);
    }

    private Event parseEvent(XMLStreamReader r, int opcode, String source, Diagnostics diag) throws XMLStreamException {
        checkAttrs(r, EVENT_ATTRS, source, diag);
        String name = required(r, "name", source, diag);
        int since = parseIntAttr(r, "since", 1, source, diag);
        OptionalInt deprecatedSince = parseOptionalIntAttr(r, "deprecated-since", source, diag);

        Description description = Description.EMPTY;
        List<Arg> args = new ArrayList<>();
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals("event")) break;
            if (ev != XMLStreamConstants.START_ELEMENT) continue;
            switch (r.getLocalName()) {
                case "description" -> description = parseDescription(r, source, diag);
                case "arg" -> args.add(parseArg(r, source, diag));
                default -> {
                    diag.warn(source, line(r), "unknown element <%s> inside <event>", r.getLocalName());
                    skipElement(r);
                }
            }
        }
        return new Event(name, opcode, since, deprecatedSince, description, args);
    }

    // ---- arg --------------------------------------------------------------

    private Arg parseArg(XMLStreamReader r, String source, Diagnostics diag) throws XMLStreamException {
        checkAttrs(r, ARG_ATTRS, source, diag);
        String name = required(r, "name", source, diag);
        ArgType type;
        String typeStr = required(r, "type", source, diag);
        try {
            type = ArgType.fromXml(typeStr);
        } catch (IllegalArgumentException e) {
            throw new ProtocolParseException(source + ":" + line(r) + ": " + e.getMessage());
        }
        String summary = attr(r, "summary");
        Optional<String> iface = Optional.ofNullable(attr(r, "interface"));
        boolean allowNull = "true".equals(attr(r, "allow-null"));
        Optional<EnumRef> enumRef = Optional.ofNullable(attr(r, "enum")).map(EnumRef::parse);
        boolean inlineNewIdInterface = type == ArgType.NEW_ID && iface.isEmpty();

        // arg is empty by DTD; consume to end
        consumeToEnd(r, "arg");
        return new Arg(name, type, summary, iface, allowNull, enumRef, inlineNewIdInterface);
    }

    // ---- enum / entry -----------------------------------------------------

    private EnumDef parseEnum(XMLStreamReader r, String source, Diagnostics diag) throws XMLStreamException {
        checkAttrs(r, ENUM_ATTRS, source, diag);
        String name = required(r, "name", source, diag);
        boolean bitfield = "true".equals(attr(r, "bitfield"));
        int since = parseIntAttr(r, "since", 1, source, diag);

        Description description = Description.EMPTY;
        List<EnumEntry> entries = new ArrayList<>();
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals("enum")) break;
            if (ev != XMLStreamConstants.START_ELEMENT) continue;
            switch (r.getLocalName()) {
                case "description" -> description = parseDescription(r, source, diag);
                case "entry" -> entries.add(parseEntry(r, source, diag));
                default -> {
                    diag.warn(source, line(r), "unknown element <%s> inside <enum>", r.getLocalName());
                    skipElement(r);
                }
            }
        }
        return new EnumDef(name, bitfield, since, description, entries);
    }

    private EnumEntry parseEntry(XMLStreamReader r, String source, Diagnostics diag) throws XMLStreamException {
        checkAttrs(r, ENTRY_ATTRS, source, diag);
        String name = required(r, "name", source, diag);
        long value = parseLongValue(required(r, "value", source, diag), source, line(r));
        int since = parseIntAttr(r, "since", 1, source, diag);
        OptionalInt deprecatedSince = parseOptionalIntAttr(r, "deprecated-since", source, diag);
        String summary = attr(r, "summary");
        Description description = Description.EMPTY;

        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals("entry")) break;
            if (ev != XMLStreamConstants.START_ELEMENT) continue;
            switch (r.getLocalName()) {
                case "description" -> description = parseDescription(r, source, diag);
                default -> {
                    diag.warn(source, line(r), "unknown element <%s> inside <entry>", r.getLocalName());
                    skipElement(r);
                }
            }
        }
        return new EnumEntry(name, value, since, deprecatedSince, summary, description);
    }

    // ---- description ------------------------------------------------------

    private Description parseDescription(XMLStreamReader r, String source, Diagnostics diag) throws XMLStreamException {
        checkAttrs(r, DESCRIPTION_ATTRS, source, diag);
        String summary = attr(r, "summary");
        String body = readElementText(r).trim();
        return new Description(summary, body);
    }

    // ---- low-level helpers -----------------------------------------------

    private static void advanceToStartElement(XMLStreamReader r) throws XMLStreamException {
        while (r.hasNext() && r.next() != XMLStreamConstants.START_ELEMENT) { /* skip */ }
    }

    private static void requireElement(XMLStreamReader r, String name) {
        if (!r.getLocalName().equals(name)) {
            throw new ProtocolParseException("expected <" + name + "> at line " + line(r) + ", got <" + r.getLocalName() + ">");
        }
    }

    private static int line(XMLStreamReader r) {
        return r.getLocation() != null ? r.getLocation().getLineNumber() : -1;
    }

    private static String attr(XMLStreamReader r, String name) {
        for (int i = 0; i < r.getAttributeCount(); i++) {
            if (r.getAttributeLocalName(i).equals(name)) {
                return r.getAttributeValue(i);
            }
        }
        return null;
    }

    private static String required(XMLStreamReader r, String name, String source, Diagnostics diag) {
        String v = attr(r, name);
        if (v == null) {
            diag.warn(source, line(r), "missing required attribute %s on <%s>", name, r.getLocalName());
            return "";
        }
        return v;
    }

    private static int parseIntAttr(XMLStreamReader r, String name, int defaultValue, String source, Diagnostics diag) {
        String v = attr(r, name);
        if (v == null) return defaultValue;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            diag.warn(source, line(r), "invalid integer attribute %s=\"%s\"", name, v);
            return defaultValue;
        }
    }

    private static OptionalInt parseOptionalIntAttr(XMLStreamReader r, String name, String source, Diagnostics diag) {
        String v = attr(r, name);
        if (v == null) return OptionalInt.empty();
        try {
            return OptionalInt.of(Integer.parseInt(v));
        } catch (NumberFormatException e) {
            diag.warn(source, line(r), "invalid integer attribute %s=\"%s\"", name, v);
            return OptionalInt.empty();
        }
    }

    private static long parseLongValue(String raw, String source, int line) {
        String s = raw.trim();
        try {
            if (s.startsWith("0x") || s.startsWith("0X")) {
                return Long.parseLong(s.substring(2), 16);
            }
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new ProtocolParseException(source + ":" + line + ": invalid numeric value \"" + raw + "\"");
        }
    }

    private static void checkAttrs(XMLStreamReader r, Set<String> known, String source, Diagnostics diag) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < r.getAttributeCount(); i++) {
            String name = r.getAttributeLocalName(i);
            if (!seen.add(name)) {
                diag.warn(source, line(r), "duplicate attribute %s on <%s>", name, r.getLocalName());
            }
            if (!known.contains(name)) {
                diag.warn(source, line(r), "unknown attribute %s on <%s>", name, r.getLocalName());
            }
        }
    }

    private static String readElementText(XMLStreamReader r) throws XMLStreamException {
        StringBuilder sb = new StringBuilder();
        String elementName = r.getLocalName();
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.CHARACTERS || ev == XMLStreamConstants.CDATA) {
                sb.append(r.getText());
            } else if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals(elementName)) {
                break;
            } else if (ev == XMLStreamConstants.START_ELEMENT) {
                throw new ProtocolParseException("unexpected nested element <" + r.getLocalName()
                        + "> inside <" + elementName + "> at line " + line(r));
            }
        }
        return sb.toString();
    }

    private static void consumeToEnd(XMLStreamReader r, String elementName) throws XMLStreamException {
        int depth = 1;
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                depth++;
            } else if (ev == XMLStreamConstants.END_ELEMENT) {
                depth--;
                if (depth == 0 && r.getLocalName().equals(elementName)) break;
            }
        }
    }

    private static void skipElement(XMLStreamReader r) throws XMLStreamException {
        consumeToEnd(r, r.getLocalName());
    }
}
