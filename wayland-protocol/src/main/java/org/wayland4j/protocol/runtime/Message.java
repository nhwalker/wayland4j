package org.wayland4j.protocol.runtime;

/**
 * Marker superinterface for every generated {@code Request} and {@code Event}
 * sealed hierarchy. Lets a connection / dispatch layer parametrise over either
 * direction without depending on a specific interface's types.
 */
public interface Message {
}
