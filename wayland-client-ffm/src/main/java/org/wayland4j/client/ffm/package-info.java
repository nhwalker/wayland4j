/**
 * Foreign Function &amp; Memory bindings for {@code libwayland-client}.
 *
 * <p>This package wraps the public C client API in
 * {@code <wayland-client.h>}. It is intentionally a thin layer: it owns
 * downcall handles, opaque-pointer wrappers, and struct layouts, but it does
 * not know anything about specific Wayland interfaces (e.g. {@code wl_surface}
 * or {@code wl_registry}). Per-interface, type-safe wrappers are produced by
 * the protocol code generator on top of this module.
 *
 * <h2>Memory model</h2>
 * The wrappers do not take ownership of the C-side allocation by default.
 * Each opaque-pointer type ({@link org.wayland4j.client.ffm.WlDisplay},
 * {@link org.wayland4j.client.ffm.WlProxy},
 * {@link org.wayland4j.client.ffm.WlEventQueue}) exposes an explicit
 * {@code close}/{@code destroy} method that calls the matching
 * {@code wl_*_destroy} or {@code wl_*_disconnect} entry point. Callers are
 * responsible for invoking it.
 *
 * <h2>Threading</h2>
 * The bindings make no extra synchronization promises beyond what
 * {@code libwayland-client} provides. See
 * {@code Documentation/Multi-thread-considerations} in the upstream source
 * for the rules around event queues and {@code wl_proxy_set_queue}.
 */
package org.wayland4j.client.ffm;
