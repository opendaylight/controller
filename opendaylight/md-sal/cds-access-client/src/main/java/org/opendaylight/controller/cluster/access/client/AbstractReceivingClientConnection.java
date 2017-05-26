/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.base.Preconditions;
import java.util.Optional;

/**
 * Implementation-internal intermediate subclass between {@link AbstractClientConnection} and two-out of three of its
 * subclasses. It allows us to share some code.
 *
 * @author Robert Varga
 *
 * @param <T> Concrete {@link BackendInfo} type
 */
abstract class AbstractReceivingClientConnection<T extends BackendInfo> extends AbstractClientConnection<T> {
    /**
     * Multiplication factor applied to remote's advertised limit on outstanding messages. Our default strategy
     * rate-limiting strategy in {@link AveragingProgressTracker} does not penalize threads as long as we have not
     * reached half of the target.
     *
     * <p>
     * By multiplying the advertised maximum by four, our queue steady-state should end up with:
     * - the backend pipeline being full,
     * - another full batch of messages being in the queue while not paying any throttling cost
     * - another 2 full batches of messages with incremental throttling cost
     */
    private static final int MESSAGE_QUEUE_FACTOR = 4;

    private final T backend;

    AbstractReceivingClientConnection(final ClientActorContext context, final Long cookie, final T backend) {
        super(context, cookie, new TransmitQueue.Transmitting(targetQueueSize(backend), backend));
        this.backend = Preconditions.checkNotNull(backend);
    }

    AbstractReceivingClientConnection(final AbstractReceivingClientConnection<T> oldConnection) {
        super(oldConnection, targetQueueSize(oldConnection.backend));
        this.backend = oldConnection.backend;
    }

    private static int targetQueueSize(final BackendInfo backend) {
        return backend.getMaxMessages() * MESSAGE_QUEUE_FACTOR;
    }

    @Override
    public final Optional<T> getBackendInfo() {
        return Optional.of(backend);
    }

    final T backend() {
        return backend;
    }
}
