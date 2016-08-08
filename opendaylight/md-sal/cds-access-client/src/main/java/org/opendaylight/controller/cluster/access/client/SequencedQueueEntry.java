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
import java.util.Optional;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single entry in {@link SequencedQueue}. Tracks the request, the associated callback and accounting information.
 *
 * @author Robert Varga
 *
 * @param <I> Target identifier type
 */
final class SequencedQueueEntry {
    private static final class LastTry {
        final long timeTicks;
        final long retry;

        LastTry(final long retry, final long timeTicks) {
            this.retry = retry;
            this.timeTicks = timeTicks;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SequencedQueueEntry.class);

    private final Request<?, ?> request;
    private final RequestCallback callback;
    private final long enqueuedTicks;

    private Optional<LastTry> lastTry = Optional.empty();

    SequencedQueueEntry(final Request<?, ?> request, final RequestCallback callback,
        final long now) {
        this.request = Preconditions.checkNotNull(request);
        this.callback = Preconditions.checkNotNull(callback);
        this.enqueuedTicks = now;
    }

    boolean acceptsResponse(final ResponseEnvelope<?> response) {
        if (!lastTry.isPresent()) {
            LOG.warn("Ignoring response {} for unsent request {}", response, request);
            return false;
        }

        final LastTry t = lastTry.get();
        return response.getTxSequence() == t.getTxSequence() && response.getSessionId() == t.getSessionId() &&
                request.getTarget().equals(response.getMessage().getTarget());
    }

    long getCurrentTry() {
        return lastTry.isPresent() ? lastTry.get().retry : 0;
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

        if (lastTry.isPresent()) {
            elapsed = now - lastTry.get().timeTicks;
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

    void retransmit(final BackendInfo backend, final long now) {
        final long retry = lastTry.isPresent() ? lastTry.get().retry + 1 : 0;
        final RequestEnvelope toSend = new RequestEnvelope(request.toVersion(backend.getVersion()), sequence, retry);

        final ActorRef actor = backend.getActor();
        LOG.trace("Retransmitting request {} as {} to {}", request, toSend, actor);
        actor.tell(toSend, ActorRef.noSender());
        lastTry = Optional.of(new LastTry(retry, now));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(SequencedQueueEntry.class).add("request", request).toString();
    }
}
