/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.WritableIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An actor context associated with this {@link AbstractClientActor}.
 *
 * Time-keeping in a client actor is based on monotonic time. The precision of this time can be expected to be the
 * same as {@link System#nanoTime()}, but it is not tied to that particular clock. Actor clock is exposed as
 * a {@link Ticker}, which can be obtained via {@link #ticker()}.
 *
 * @author Robert Varga
 */
@Beta
@ThreadSafe
public class ClientActorContext extends AbstractClientActorContext implements Identifiable<ClientIdentifier> {
    private static final Logger LOG = LoggerFactory.getLogger(ClientActorContext.class);

    private final Map<Identifier, SequencedQueue> requests = new ConcurrentHashMap<>();
    private final ClientIdentifier identifier;

    // Hidden to avoid subclassing
    ClientActorContext(final ActorRef self, final String persistenceId, final ClientIdentifier identifier) {
        super(self, persistenceId);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    @Override
    public @Nonnull ClientIdentifier getIdentifier() {
        return identifier;
    }

    /**
     * Return the time ticker for this {@link ClientActorContext}. This should be used for in all time-tracking
     * down within a client actor. Subclasses of {@link ClientActorBehavior} are encouraged to use
     * {@link com.google.common.base.Stopwatch}.
     *
     * @return Client actor time source
     */
    public @Nonnull Ticker ticker() {
        return Ticker.systemTicker();
    }

    /**
     * Execute a command in the context of the client actor.
     *
     * @param command Block of code which needs to be execute
     */
    public void executeInActor(final @Nonnull InternalCommand command) {
        self().tell(Preconditions.checkNotNull(command), ActorRef.noSender());
    }

    <I extends WritableIdentifier, T extends Request<I, T>> void addRequest(final T request,
            final RequestCallback<I> onResponse) {
        final SequencedQueue queue = requests.computeIfAbsent(request.getTarget(), t -> new SequencedQueue(ticker()));
        queue.add(request, onResponse, ticker().read());
    }

    ClientActorBehavior completeRequest(final ClientActorBehavior current, final Response<?, ?> response) {
        final SequencedQueue queue = requests.get(response.getTarget());
        if (queue == null) {
            LOG.info("{}: Ignoring unknown response {}", persistenceId(), response);
            return current;
        } else {
            return queue.complete(current, response);
        }
    }

    void retryRequest(final RequestFailure<?, ?> failure, final BackendInfoResolver<?> resolver) {
        final SequencedQueue queue = requests.get(failure.getTarget());
        if (queue == null) {
            LOG.info("{}: Ignoring unknown response {}", persistenceId(), failure);
            return;
        }

        queue.retryRequest(failure, resolver);
    }


    long timeoutRequests(final long tickNanos, final Consumer<Entry<Request<?, ?>, RequestCallback<?>>> callback) {
        Preconditions.checkArgument(tickNanos > 0, "Next tick duration must be greater than 0, was %s", tickNanos);
        final long now = ticker().read();
        long next = Long.MAX_VALUE;

        final Iterator<SequencedQueue> it = requests.values().iterator();
        while (it.hasNext()) {
            final SequencedQueue q = it.next();

            // Nanoseconds until next timeout (as measured from now)
            final long nextTimeout = q.processTimeouts(now, tickNanos, callback);
            if (nextTimeout == Long.MIN_VALUE) {
                it.remove();
            } else if (next > nextTimeout) {
                next = nextTimeout;

            }
        }

        // next deadline
        LOG.trace("Next deadline in {}ns", next);
        return now + next;
    }
}
