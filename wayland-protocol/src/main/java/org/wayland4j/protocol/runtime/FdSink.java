package org.wayland4j.protocol.runtime;

/**
 * Side-channel sink for file descriptors. Generated message writers push fds
 * here instead of writing them inline; the transport layer attaches them to an
 * outgoing message via SCM_RIGHTS (out of scope in v1).
 */
public interface FdSink {

    void push(int fd);
}
