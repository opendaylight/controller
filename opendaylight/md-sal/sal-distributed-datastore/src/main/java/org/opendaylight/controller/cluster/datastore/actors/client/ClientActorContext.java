/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Scheduler;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.concepts.WritableIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.FiniteDuration;

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
    private static final FiniteDuration REQUEST_TIMEOUT = FiniteDuration.apply(30, TimeUnit.SECONDS);

    private final Map<Identifier, SequencedQueue> requests = new ConcurrentHashMap<>();
    private final ClientIdentifier identifier;
    private final ExecutionContext executionContext;
    private final Scheduler scheduler;

    // Hidden to avoid subclassing
    ClientActorContext(final ActorRef self, final Scheduler scheduler, final ExecutionContext executionContext,
            final String persistenceId, final ClientIdentifier identifier) {
        super(self, persistenceId);
        this.identifier = Preconditions.checkNotNull(identifier);
        this.scheduler = Preconditions.checkNotNull(scheduler);
        this.executionContext = Preconditions.checkNotNull(executionContext);
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

    public Cancellable executeInActor(final @Nonnull InternalCommand command, final FiniteDuration delay) {
        return scheduler.scheduleOnce(Preconditions.checkNotNull(delay), self(), Preconditions.checkNotNull(command),
            executionContext, ActorRef.noSender());
    }

    <I extends WritableIdentifier, T extends Request<I, T>> void addRequest(final T request,
            final RequestCallback<I> onResponse) {
        final SequencedQueue queue = requests.computeIfAbsent(request.getTarget(), t -> new SequencedQueue(ticker()));
        if (queue.add(request, onResponse, this)) {
            executeInActor(cb -> handleTimeouts(cb, queue), REQUEST_TIMEOUT);
        }
    }

    private static ClientActorBehavior handleTimeouts(final ClientActorBehavior currentBehavior, final SequencedQueue queue) {
        queue.retryRequests(currentBehavior);
        return currentBehavior;
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
}
