module org.wayland4j.protocol {
    requires java.xml;
    requires java.compiler;

    exports org.wayland4j.protocol.model;
    exports org.wayland4j.protocol.scanner;
    exports org.wayland4j.protocol.apt;

    provides javax.annotation.processing.Processor
            with org.wayland4j.protocol.apt.WaylandClientApProcessor;
}
