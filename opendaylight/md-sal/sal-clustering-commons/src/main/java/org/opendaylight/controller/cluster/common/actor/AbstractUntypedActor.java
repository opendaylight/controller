/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractUntypedActor extends AbstractActor implements ExecuteInSelfActor {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractUntypedActor.class);

    protected @NonNull String logName;

    protected AbstractUntypedActor(final @NonNull String logName) {
        this.logName = requireNonNull(logName);
        LOG.debug("{}: Actor created {}", logName, self());
        getContext().system().actorSelection("user/termination-monitor").tell(new Monitor(self()), self());
    }

    @Override
    public final ActorContext getContext() {
        return super.getContext();
    }

    @Override
    public final void executeInSelf(final Runnable runnable) {
        final ExecuteInSelfMessage message = new ExecuteInSelfMessage(runnable);
        self().tell(message, ActorRef.noSender());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(ExecuteInSelfMessage.class, ExecuteInSelfMessage::run)
            .matchAny(this::handleReceive)
            .build();
    }

    /**
     * Receive and handle an incoming message. If the implementation does not handle this particular message,
     * it should call {@link #ignoreMessage(Object)} or {@link #unknownMessage(Object)}.
     *
     * @param message the incoming message
     */
    protected abstract void handleReceive(Object message);

    protected final void ignoreMessage(final Object message) {
        LOG.debug("{}: Ignoring unhandled message {}", logName, message);
    }

    protected final void unknownMessage(final Object message) {
        LOG.debug("{}: Received unhandled message {}", logName, message);
        unhandled(message);
    }

    protected boolean isValidSender(final ActorRef sender) {
        // If the caller passes in a null sender (ActorRef.noSender()), akka translates that to the
        // deadLetters actor.
        return sender != null && !getContext().system().deadLetters().equals(sender);
    }
}
