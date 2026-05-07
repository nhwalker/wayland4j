package org.wayland4j.protocol.runtime;

import java.util.NoSuchElementException;

/**
 * Side-channel source of file descriptors. Generated message readers pop fds
 * from here when an {@code fd} arg is parsed; the transport layer fills it in
 * from SCM_RIGHTS ancillary data (out of scope in v1).
 */
public interface FdSource {

    /**
     * @return the next file descriptor in arrival order
     * @throws NoSuchElementException if no fd is available
     */
    int pop();
}
