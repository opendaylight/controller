package org.opendaylight.controller.config.yang.test.impl;

import com.google.common.base.Preconditions;

public class CheckedAutoCloseable implements AutoCloseable {
    private boolean closed = false;

    @Override
    public synchronized void close() throws Exception {
        Preconditions.checkState(closed == false);
        this.closed = true;
    }

    public synchronized boolean isClosed() {
        return this.closed;
    }
}
