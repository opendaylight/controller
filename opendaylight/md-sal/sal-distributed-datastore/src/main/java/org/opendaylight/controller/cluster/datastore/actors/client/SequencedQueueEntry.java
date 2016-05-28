/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import akka.actor.ActorRef;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.util.Optional;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.Response;
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
        final Request<?, ?> request;
        final long timeTicks;

        LastTry(final Request<?, ?> request, final long when) {
            this.request = Preconditions.checkNotNull(request);
            this.timeTicks = when;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SequencedQueueEntry.class);

    private final Request<?, ?> request;
    private final RequestCallback callback;
    private final long enqueuedTicks;

    private Optional<LastTry> lastTry = Optional.empty();

    SequencedQueueEntry(final Request<?, ?> request, final RequestCallback callback, final long now) {
        this.request = Preconditions.checkNotNull(request);
        this.callback = Preconditions.checkNotNull(callback);
        this.enqueuedTicks = now;
    }

    long getSequence() {
        return request.getSequence();
    }

    boolean acceptsResponse(final Response<?, ?> response) {
        return getSequence() == response.getSequence() && request.getTarget().equals(response.getTarget());
    }

    long getCurrentTry() {
        final Request<?, ?> req = lastTry.isPresent() ? lastTry.get().request : request;
        return req.getRetry();
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
        final Request<?, ?> req;
        final long elapsed;

        if (lastTry.isPresent()) {
            final LastTry t = lastTry.get();
            elapsed = now - t.timeTicks;
            req = t.request;
        } else {
            elapsed = now - enqueuedTicks;
            req = request;
        }

        if (elapsed >= timeoutNanos) {
            LOG.debug("Request {} timed out after {}ns", req, elapsed);
            return true;
        } else {
            return false;
        }
    }

    void retransmit(final BackendInfo backend, final long now) {
        final Request<?, ?> nextTry = lastTry.isPresent() ? lastTry.get().request.incrementRetry() : request;
        final Request<?, ?> toSend = nextTry.toVersion(backend.getVersion());
        final ActorRef actor = backend.getActor();

        LOG.trace("Retransmitting request {} as {} to {}", request, toSend, actor);
        actor.tell(toSend, ActorRef.noSender());
        lastTry = Optional.of(new LastTry(toSend, now));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(SequencedQueueEntry.class).add("request", request).toString();
    }
}
