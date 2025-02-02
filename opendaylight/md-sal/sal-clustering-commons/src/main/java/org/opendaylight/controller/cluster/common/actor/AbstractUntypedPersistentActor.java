/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.persistence.AbstractPersistentActor;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractUntypedPersistentActor extends AbstractPersistentActor implements ExecuteInSelfActor {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractUntypedPersistentActor.class);

    private final @NonNull String persistenceId;

    protected AbstractUntypedPersistentActor(final String persistenceId) {
        this.persistenceId = requireNonNull(persistenceId);
        LOG.trace("{}: Actor created {}", persistenceId, self());
        getContext().system().actorSelection("user/termination-monitor").tell(new Monitor(self()), self());
    }

    @Override
    @Deprecated(since = "11.0.0", forRemoval = true)
    public final String persistenceId() {
        return persistenceId;
    }

    @Override
    public final ActorContext getContext() {
        return super.getContext();
    }

    @Override
    public final void executeInSelf(@NonNull final Runnable runnable) {
        final ExecuteInSelfMessage message = new ExecuteInSelfMessage(runnable);
        LOG.trace("{}: Scheduling execution of {}", persistenceId, message);
        self().tell(message, ActorRef.noSender());
    }

    @Override
    public final Receive createReceive() {
        return receiveBuilder()
            .match(ExecuteInSelfMessage.class, ExecuteInSelfMessage::run)
            .matchAny(this::handleCommand)
            .build();
    }

    @Override
    public final Receive createReceiveRecover() {
        return receiveBuilder().matchAny(this::handleRecover).build();
    }

    protected abstract void handleRecover(Object message) throws Exception;

    protected abstract void handleCommand(Object message) throws Exception;

    protected void ignoreMessage(final Object message) {
        LOG.debug("{}: Unhandled message {}", persistenceId, message);
    }

    protected void unknownMessage(final Object message) {
        LOG.debug("{}: Received unhandled message {}", persistenceId, message);
        unhandled(message);
    }
}
