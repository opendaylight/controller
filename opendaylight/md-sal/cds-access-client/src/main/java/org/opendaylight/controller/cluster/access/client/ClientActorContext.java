/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Scheduler;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
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

    private final Map<Long, SequencedQueue> queues = new ConcurrentHashMap<>();
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
     * done within a client actor. Subclasses of {@link ClientActorBehavior} are encouraged to use
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

    SequencedQueue queueFor(final Long cookie) {
        return queues.computeIfAbsent(cookie, t -> new SequencedQueue(t, ticker()));
    }

    void removeQueue(final SequencedQueue queue) {
        queues.remove(queue.getCookie(), queue);
    }

    ClientActorBehavior completeRequest(final ClientActorBehavior current, final ResponseEnvelope<?> response) {
        final WritableIdentifier id = response.getMessage().getTarget();

        // FIXME: this will need to be updated for other Request/Response types to extract cookie
        Preconditions.checkArgument(id instanceof TransactionIdentifier);
        final TransactionIdentifier txId = (TransactionIdentifier) id;

        final SequencedQueue queue = queues.get(txId.getHistoryId().getCookie());
        if (queue == null) {
            LOG.info("{}: Ignoring unknown response {}", persistenceId(), response);
            return current;
        } else {
            return queue.complete(current, response);
        }
    }

    void poison(final RequestException cause) {
        for (SequencedQueue q : queues.values()) {
            q.poison(cause);
        }

        queues.clear();
    }
}
