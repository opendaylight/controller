package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import java.util.concurrent.CountDownLatch;

final class InversibleLockException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final CountDownLatch latch;

    InversibleLockException(final CountDownLatch latch) {
        this.latch = Preconditions.checkNotNull(latch);
    }

    void awaitResolution() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while waiting for latch " + latch, e);
        }
    }
}
