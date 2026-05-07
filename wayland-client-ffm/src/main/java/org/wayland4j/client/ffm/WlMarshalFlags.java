package org.wayland4j.client.ffm;

/**
 * Flags accepted by {@code wl_proxy_marshal_flags} /
 * {@code wl_proxy_marshal_array_flags}.
 *
 * <p>These mirror the {@code WL_MARSHAL_FLAG_*} macros declared in
 * {@code <wayland-client-core.h>}.
 */
public final class WlMarshalFlags {

    private WlMarshalFlags() {}

    /** Destroy the proxy after sending the request. */
    public static final int DESTROY = 1 << 0;
}
