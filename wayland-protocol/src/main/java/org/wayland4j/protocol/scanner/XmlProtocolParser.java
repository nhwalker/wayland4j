package org.wayland4j.protocol.scanner;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wayland4j.protocol.model.Arg;
import org.wayland4j.protocol.model.ArgType;
import org.wayland4j.protocol.model.EnumDef;
import org.wayland4j.protocol.model.EnumEntry;
import org.wayland4j.protocol.model.Event;
import org.wayland4j.protocol.model.Interface;
import org.wayland4j.protocol.model.Protocol;
import org.wayland4j.protocol.model.Request;

public final class XmlProtocolParser {

    private XmlProtocolParser() {
    }

    public static Protocol parse(InputStream xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> new org.xml.sax.InputSource(new java.io.StringReader("")));
            Document doc = builder.parse(xml);
            return parseProtocol(doc.getDocumentElement());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("failed to parse wayland protocol XML", e);
        }
    }

    private static Protocol parseProtocol(Element protocolEl) {
        String name = protocolEl.getAttribute("name");
        String copyright = textOf(child(protocolEl, "copyright"));
        String description = parseDescription(child(protocolEl, "description"));
        List<Interface> interfaces = new ArrayList<>();
        for (Element ifaceEl : children(protocolEl, "interface")) {
            interfaces.add(parseInterface(ifaceEl));
        }
        return new Protocol(name, copyright, description, List.copyOf(interfaces));
    }

    private static Interface parseInterface(Element el) {
        String name = el.getAttribute("name");
        int version = Integer.parseInt(el.getAttribute("version"));
        String description = parseDescription(child(el, "description"));
        List<Request> requests = new ArrayList<>();
        int reqOpcode = 0;
        for (Element reqEl : children(el, "request")) {
            requests.add(parseRequest(reqEl, reqOpcode++));
        }
        List<Event> events = new ArrayList<>();
        int evOpcode = 0;
        for (Element evEl : children(el, "event")) {
            events.add(parseEvent(evEl, evOpcode++));
        }
        List<EnumDef> enums = new ArrayList<>();
        for (Element enEl : children(el, "enum")) {
            enums.add(parseEnum(enEl));
        }
        return new Interface(name, version, description, List.copyOf(requests), List.copyOf(events), List.copyOf(enums));
    }

    private static Request parseRequest(Element el, int opcode) {
        String name = el.getAttribute("name");
        Integer since = optInt(el, "since");
        boolean destructor = "destructor".equals(el.getAttribute("type"));
        String description = parseDescription(child(el, "description"));
        return new Request(name, opcode, since, destructor, description, parseArgs(el));
    }

    private static Event parseEvent(Element el, int opcode) {
        String name = el.getAttribute("name");
        Integer since = optInt(el, "since");
        String description = parseDescription(child(el, "description"));
        return new Event(name, opcode, since, description, parseArgs(el));
    }

    private static List<Arg> parseArgs(Element parent) {
        List<Arg> args = new ArrayList<>();
        for (Element a : children(parent, "arg")) {
            String name = a.getAttribute("name");
            ArgType type = ArgType.fromXml(a.getAttribute("type"));
            String iface = nullIfEmpty(a.getAttribute("interface"));
            boolean nullable = "true".equals(a.getAttribute("allow-null"));
            String enumRef = nullIfEmpty(a.getAttribute("enum"));
            String summary = nullIfEmpty(a.getAttribute("summary"));
            args.add(new Arg(name, type, iface, nullable, enumRef, summary));
        }
        return List.copyOf(args);
    }

    private static EnumDef parseEnum(Element el) {
        String name = el.getAttribute("name");
        Integer since = optInt(el, "since");
        boolean bitfield = "true".equals(el.getAttribute("bitfield"));
        String description = parseDescription(child(el, "description"));
        List<EnumEntry> entries = new ArrayList<>();
        for (Element e : children(el, "entry")) {
            String entryName = e.getAttribute("name");
            long value = parseEnumValue(e.getAttribute("value"));
            Integer entrySince = optInt(e, "since");
            String summary = nullIfEmpty(e.getAttribute("summary"));
            entries.add(new EnumEntry(entryName, value, entrySince, summary));
        }
        return new EnumDef(name, since, bitfield, description, List.copyOf(entries));
    }

    private static long parseEnumValue(String s) {
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return Long.parseUnsignedLong(s.substring(2), 16);
        }
        return Long.parseLong(s);
    }

    private static String parseDescription(Element el) {
        if (el == null) return null;
        String summary = el.getAttribute("summary");
        String body = textOf(el);
        if ((summary == null || summary.isEmpty()) && (body == null || body.isBlank())) return null;
        StringBuilder sb = new StringBuilder();
        if (summary != null && !summary.isEmpty()) sb.append(summary);
        if (body != null && !body.isBlank()) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append(body.strip());
        }
        return sb.toString();
    }

    private static Element child(Element parent, String name) {
        NodeList kids = parent.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node n = kids.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals(name)) {
                return (Element) n;
            }
        }
        return null;
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> out = new ArrayList<>();
        NodeList kids = parent.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node n = kids.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals(name)) {
                out.add((Element) n);
            }
        }
        return out;
    }

    private static String textOf(Node n) {
        if (n == null) return null;
        StringBuilder sb = new StringBuilder();
        NodeList kids = n.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node k = kids.item(i);
            if (k.getNodeType() == Node.TEXT_NODE || k.getNodeType() == Node.CDATA_SECTION_NODE) {
                sb.append(k.getNodeValue());
            }
        }
        return sb.toString();
    }

    private static Integer optInt(Element el, String attr) {
        String v = el.getAttribute(attr);
        return (v == null || v.isEmpty()) ? null : Integer.parseInt(v);
    }

    private static String nullIfEmpty(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }
}
