/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.util.Optional;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation-internal intermediate subclass between {@link AbstractClientConnection} and two-out of three of its
 * subclasses. It allows us to share some code.
 *
 * @author Robert Varga
 *
 * @param <T> Concrete {@link BackendInfo} type
 */
abstract class AbstractReceivingClientConnection<T extends BackendInfo> extends AbstractClientConnection<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractReceivingClientConnection.class);

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

    // To be called by ConnectedClientConnection only.
    AbstractReceivingClientConnection(final AbstractClientConnection<T> oldConnection, final T newBackend) {
        super(oldConnection, newBackend, targetQueueSize(newBackend));
        this.backend = newBackend;
    }

    // To be called by ReconnectingClientConnection only.
    AbstractReceivingClientConnection(final AbstractReceivingClientConnection<T> oldConnection) {
        super(oldConnection);
        this.backend = oldConnection.backend;
    }

    private static int targetQueueSize(final BackendInfo backend) {
        return backend.getMaxMessages() * MESSAGE_QUEUE_FACTOR;
    }

    @Override
    public final Optional<T> getBackendInfo() {
        return Optional.of(backend);
    }

    @Override
    final void receiveResponse(final ResponseEnvelope<?> envelope) {
        if (envelope.getSessionId() != backend.getSessionId()) {
            LOG.debug("Response {} does not match session ID {}, ignoring it", envelope, backend.getSessionId());
        } else {
            super.receiveResponse(envelope);
        }
    }

    final T backend() {
        return backend;
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("backend", backend);
    }
}
