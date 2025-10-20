/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractUntypedActor extends AbstractActor implements ExecuteInSelfActor {
    // The member name should be lower case but it's referenced in many subclasses. Suppressing the CS warning for now.
    @SuppressWarnings("checkstyle:MemberName")
    @SuppressFBWarnings(value = "SLF4J_LOGGER_SHOULD_BE_PRIVATE", justification = "Class identity is required")
    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    protected AbstractUntypedActor() {
        LOG.debug("Actor created {}", self());
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
        LOG.debug("Ignoring unhandled message {}", message);
    }

    protected final void unknownMessage(final Object message) {
        LOG.debug("Received unhandled message {}", message);
        unhandled(message);
    }

    protected boolean isValidSender(final ActorRef sender) {
        // If the caller passes in a null sender (ActorRef.noSender()), akka translates that to the
        // deadLetters actor.
        return sender != null && !getContext().system().deadLetters().equals(sender);
    }
}
