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

    /**
     * Marks the package as containing the core Wayland protocol. Core packages
     * are loaded before extensions so cross-protocol {@code wl_surface}-style
     * type references resolve. Set on the package that hosts {@code wayland.xml}.
     */
    boolean core() default false;
}
