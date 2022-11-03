/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static java.util.Objects.requireNonNull;

import java.io.Serial;
import java.util.concurrent.CountDownLatch;

/**
 * Exception thrown from {@link InversibleLock#optimisticRead()} and can be used to wait for the racing write
 * to complete using {@link #awaitResolution()}.
 */
public final class InversibleLockException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final transient CountDownLatch latch;

    InversibleLockException(final CountDownLatch latch) {
        this.latch = requireNonNull(latch);
    }

    public void awaitResolution() {
        // latch can be null after deserialization
        if (latch != null) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException("Interrupted while waiting for latch " + latch, e);
            }
        }
    }
}
