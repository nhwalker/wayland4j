package org.wayland4j.client;

import java.lang.foreign.MemorySegment;

/**
 * Untyped wrapper used when the dispatcher decodes an {@code o} argument
 * pointing at a proxy that wayland4j has no Java metadata for — most commonly
 * {@code wl_display.error.object_id} when the failing interface isn't part of
 * any loaded protocol bundle. Provides {@link Proxy#id()},
 * {@link Proxy#version()}, and {@link Proxy#wlClassName()} via libwayland;
 * cannot send requests.
 *
 * <p>Lifetime is owned by libwayland, not this wrapper. Do not destroy.
 */
public final class OpaqueProxy extends Proxy {

    public OpaqueProxy(MemorySegment ptr) {
        super(ptr);
    }
}
