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

    public DispatchTable(EventDispatcher[] events) {
        this.events = events;
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
}
