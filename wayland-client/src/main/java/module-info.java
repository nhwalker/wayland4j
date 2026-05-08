module org.wayland4j.client {
    requires java.logging;
    requires org.wayland4j.protocol;

    exports org.wayland4j.client;
    exports org.wayland4j.client.protocol;
    exports org.wayland4j.client.xdgshell;
    exports org.wayland4j.client.viewporter;
    exports org.wayland4j.client.presentation;
    exports org.wayland4j.client.linuxdmabuf;
    exports org.wayland4j.client.tablet;
    // org.wayland4j.client.internal is intentionally not exported.
}
