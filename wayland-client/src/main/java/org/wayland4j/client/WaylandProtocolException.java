package org.wayland4j.client;

import java.io.IOException;

/**
 * Thrown when the compositor returns a {@code wl_display.error} event or any
 * libwayland-client API reports a fatal protocol error.
 */
public final class WaylandProtocolException extends IOException {

    private static final long serialVersionUID = 1L;

    private final String interfaceName;
    private final int objectId;
    private final int errorCode;

    public WaylandProtocolException(String interfaceName, int objectId, int errorCode, String message) {
        super(formatMessage(interfaceName, objectId, errorCode, message));
        this.interfaceName = interfaceName;
        this.objectId = objectId;
        this.errorCode = errorCode;
    }

    public String interfaceName() { return interfaceName; }
    public int objectId() { return objectId; }
    public int errorCode() { return errorCode; }

    private static String formatMessage(String iface, int id, int code, String msg) {
        StringBuilder sb = new StringBuilder("Wayland protocol error");
        if (iface != null) sb.append(" on ").append(iface).append('@').append(id);
        sb.append(" code=").append(code);
        if (msg != null && !msg.isEmpty()) sb.append(": ").append(msg);
        return sb.toString();
    }
}
