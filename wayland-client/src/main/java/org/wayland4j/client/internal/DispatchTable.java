package org.wayland4j.client.internal;

import org.wayland4j.client.Proxy;

/**
 * Per-interface table of event dispatchers, indexed by opcode. Generated proxy
 * classes own one of these.
 */
public final class DispatchTable {

    @FunctionalInterface
    public interface EventDispatcher {
        void dispatch(Proxy proxy, Object listener, Object[] args);
    }

    private final EventDispatcher[] events;
    private final boolean[] destructorEvents;

    public DispatchTable(EventDispatcher[] events) {
        this(events, new boolean[events.length]);
    }

    public DispatchTable(EventDispatcher[] events, boolean[] destructorEvents) {
        if (events.length != destructorEvents.length) {
            throw new IllegalArgumentException(
                    "events/destructor flag length mismatch: " + events.length + " vs " + destructorEvents.length);
        }
        this.events = events;
        this.destructorEvents = destructorEvents;
    }

    public int eventCount() {
        return events.length;
    }

    public EventDispatcher dispatcherFor(int opcode) {
        if (opcode < 0 || opcode >= events.length) {
            throw new IllegalArgumentException(
                    "opcode " + opcode + " out of range (events=" + events.length + ")");
        }
        return events[opcode];
    }

    public boolean isDestructor(int opcode) {
        if (opcode < 0 || opcode >= destructorEvents.length) {
            return false;
        }
        return destructorEvents[opcode];
    }
}
