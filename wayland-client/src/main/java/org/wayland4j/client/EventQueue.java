package org.wayland4j.client;

import java.lang.foreign.MemorySegment;
import org.wayland4j.client.internal.NativeLibrary;

/**
 * Thin wrapper over libwayland's {@code wl_event_queue}. Used to route a
 * subset of a {@link Display}'s incoming events to a different dispatch
 * thread or schedule. Typical usage: create a queue, attach proxies via
 * {@link Proxy#setQueue(EventQueue)}, drive
 * {@link Display#dispatchQueue(EventQueue)} from a background thread.
 *
 * <p>Only one thread may drive a given queue at a time. wayland4j adds no
 * locks of its own — the same single-owner-per-queue contract as libwayland.
 */
public final class EventQueue implements AutoCloseable {

    private final MemorySegment ptr;
    private final String name;
    private volatile boolean closed = false;

    EventQueue(MemorySegment ptr, String name) {
        if (ptr == null || ptr.address() == 0L) {
            throw new IllegalArgumentException("null wl_event_queue");
        }
        this.ptr = ptr;
        this.name = name;
    }

    public MemorySegment ptr() {
        return ptr;
    }

    public long address() {
        return ptr.address();
    }

    /** Optional name (only used by libwayland with {@code wl_display_create_queue_with_name}). */
    public String name() {
        return name;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            NativeLibrary.WL_EVENT_QUEUE_DESTROY.invokeExact(ptr);
        } catch (Throwable t) {
            if (t instanceof RuntimeException re) throw re;
            if (t instanceof Error er) throw er;
            throw new RuntimeException(t);
        }
    }

    @Override
    public String toString() {
        return "EventQueue[" + (name == null ? "0x" + Long.toHexString(address()) : name) + "]";
    }
}
