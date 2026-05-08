package org.wayland4j.protocol.apt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation that triggers generation of typed Wayland proxies from a
 * vendored XML protocol file. Place on a {@code package-info.java}; the
 * processor emits one class per {@code <interface>} into the same package.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PACKAGE)
public @interface GenerateWaylandClient {

    /**
     * Classpath resource path for the protocol XML, e.g.
     * {@code "org/wayland4j/protocol/wayland.xml"}.
     */
    String xml();
}
