/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import akka.actor.Status;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.datastore.actors.client.ClientActorBehavior;
import org.opendaylight.controller.cluster.datastore.actors.client.ClientActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ClientActorBehavior} acting as an intermediary between the backend actors and the DistributedDataStore
 * frontend.
 *
 * This class is not visible outside of this package because it breaks the actor containment. Services provided to
 * Java world outside of actor containment are captured in {@link DistributedDataStoreClient}.
 *
 * IMPORTANT: this class breaks actor containment via methods implementing {@link DistributedDataStoreClient} contract.
 *            When touching internal state, be mindful of the execution context from which execution context, Actor
 *            or POJO, is the state being accessed or modified.
 *
 * THREAD SAFETY: this class must always be kept thread-safe, so that both the Actor System thread and the application
 *                threads can run concurrently. All state transitions must be made in a thread-safe manner. When in
 *                doubt, feel free to synchronize on this object.
 *
 * PERFORMANCE: this class lies in a performance-critical fast path. All code needs to be concise and efficient, but
 *              performance must not come at the price of correctness. Any optimizations need to be carefully analyzed
 *              for correctness and performance impact.
 *
 * TRADE-OFFS: part of the functionality runs in application threads without switching contexts, which makes it ideal
 *             for performing work and charging applications for it. That has two positive effects:
 *             - CPU usage is distributed across applications, minimizing work done in the actor thread
 *             - CPU usage provides back-pressure towards the application.
 *
 * @author Robert Varga
 */
final class DistributedDataStoreClientBehavior extends ClientActorBehavior implements DistributedDataStoreClient {
    private static final Logger LOG = LoggerFactory.getLogger(DistributedDataStoreClientBehavior.class);
    private static final Object SHUTDOWN = new Object() {
        @Override
        public String toString() {
            return "SHUTDOWN";
        }
    };

    private long nextHistoryId;

    DistributedDataStoreClientBehavior(final ClientActorContext context) {
        super(context);
    }

    //
    //
    // Methods below are invoked from the client actor thread
    //
    //

    private void createLocalHistory(final CreateLocalHistoryCommand command) {
        final CompletableFuture<ClientLocalHistory> future = command.future();
        final LocalHistoryIdentifier historyId = new LocalHistoryIdentifier(getIdentifier(), nextHistoryId++);
        LOG.debug("{}: creating a new local history {} for {}", persistenceId(), historyId, future);

        // FIXME: initiate backend instantiation
        future.completeExceptionally(new UnsupportedOperationException("Not implemented yet"));
    }

    @Override
    protected ClientActorBehavior onCommand(final Object command) {
        if (command instanceof CreateLocalHistoryCommand) {
            createLocalHistory((CreateLocalHistoryCommand) command);
        } else if (command instanceof GetClientRequest) {
            ((GetClientRequest) command).getReplyTo().tell(new Status.Success(this), ActorRef.noSender());
        } else if (SHUTDOWN.equals(command)) {
            // Add shutdown procedures here
            return null;
        } else {
            LOG.warn("{}: ignoring unhandled command {}", persistenceId(), command);
        }

        return this;
    }

    @Override
    protected void haltClient(final Throwable cause) {
        // Add state flushing here once we have state
    }

    //
    //
    // Methods below are invoked from application threads
    //
    //

    @Override
    public CompletionStage<ClientLocalHistory> createLocalHistory() {
        final CreateLocalHistoryCommand command = new CreateLocalHistoryCommand();
        self().tell(command, ActorRef.noSender());
        return command.future();
    }

    @Override
    public void close() {
        self().tell(SHUTDOWN, ActorRef.noSender());
    }
}
