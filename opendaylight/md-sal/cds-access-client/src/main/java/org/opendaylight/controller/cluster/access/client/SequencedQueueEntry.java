/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.actor.ActorRef;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single entry in {@link SequencedQueue}. Tracks the request, the associated callback and accounting information.
 *
 * @author Robert Varga
 */
final class SequencedQueueEntry {
    private static final Logger LOG = LoggerFactory.getLogger(SequencedQueueEntry.class);

    private final Request<?, ?> request;
    private final RequestCallback callback;
    private final long enqueuedTicks;

    private TxDetails txDetails;

    SequencedQueueEntry(final Request<?, ?> request, final RequestCallback callback,
        final long now) {
        this.request = Preconditions.checkNotNull(request);
        this.callback = Preconditions.checkNotNull(callback);
        this.enqueuedTicks = now;
    }

    Request<?, ?> getRequest() {
        return request;
    }

    @Nullable TxDetails getTxDetails() {
        return txDetails;
    }

    ClientActorBehavior complete(final Response<?, ?> response) {
        LOG.debug("Completing request {} with {}", request, response);
        return callback.complete(response);
    }

    void poison(final RequestException cause) {
        LOG.trace("Poisoning request {}", request, cause);
        callback.complete(request.toRequestFailure(cause));
    }

    boolean isTimedOut(final long now, final long timeoutNanos) {
        final long elapsed;

        if (txDetails != null) {
            elapsed = now - txDetails.getTimeTicks();
        } else {
            elapsed = now - enqueuedTicks;
        }

        if (elapsed >= timeoutNanos) {
            LOG.debug("Request {} timed out after {}ns", request, elapsed);
            return true;
        } else {
            return false;
        }
    }

    void retransmit(final BackendInfo backend, final long txSequence, final long now) {
        final RequestEnvelope toSend = new RequestEnvelope(request.toVersion(backend.getVersion()),
            backend.getSessionId(), txSequence);

        final ActorRef actor = backend.getActor();
        LOG.trace("Transmitting request {} as {} to {}", request, toSend, actor);
        actor.tell(toSend, ActorRef.noSender());
        txDetails = new TxDetails(backend.getSessionId(), txSequence, now);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(SequencedQueueEntry.class).add("request", request).toString();
    }

}
