package org.wayland4j.client.ffm;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Objects;

/**
 * Wrapper for {@code struct wl_event_queue *}, the per-thread queue used
 * to multiplex incoming events. Created by
 * {@link WlDisplay#createQueue()} and torn down by {@link #close()}
 * (which calls {@code wl_event_queue_destroy}).
 */
public final class WlEventQueue implements AutoCloseable {

    private static final MethodHandle WL_EVENT_QUEUE_DESTROY = WaylandNative.downcall(
            "wl_event_queue_destroy",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    private final MemorySegment handle;
    private boolean destroyed;

    WlEventQueue(MemorySegment handle) {
        this.handle = Objects.requireNonNull(handle);
    }

    /** The underlying {@code wl_event_queue *} address. */
    public MemorySegment address() {
        return handle;
    }

    /** {@code wl_event_queue_destroy}. Idempotent. */
    @Override
    public void close() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        try {
            WL_EVENT_QUEUE_DESTROY.invokeExact(handle);
        } catch (Throwable t) {
            throw new WaylandClientException("wl_event_queue_destroy failed", t);
        }
    }
}
