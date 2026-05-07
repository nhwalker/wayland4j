package org.wayland4j.client.ffm;

/**
 * Thrown when {@code libwayland-client} reports an error or when an
 * argument cannot be coerced to the C ABI.
 */
public class WaylandClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WaylandClientException(String message) {
        super(message);
    }

    public WaylandClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
