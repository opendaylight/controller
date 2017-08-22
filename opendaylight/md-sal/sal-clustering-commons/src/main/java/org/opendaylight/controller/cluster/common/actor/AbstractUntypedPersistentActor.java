/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import akka.actor.ActorRef;
import akka.persistence.UntypedPersistentActor;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractUntypedPersistentActor extends UntypedPersistentActor implements ExecuteInSelfActor {

    // The member name should be lower case but it's referenced in many subclasses. Suppressing the CS warning for now.
    @SuppressWarnings("checkstyle:MemberName")
    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    protected AbstractUntypedPersistentActor() {
        LOG.trace("Actor created {}", getSelf());
        getContext().system().actorSelection("user/termination-monitor").tell(new Monitor(getSelf()), getSelf());
    }

    @Override
    public final void executeInSelf(@NonNull final Runnable runnable) {
        final ExecuteInSelfMessage message = new ExecuteInSelfMessage(runnable);
        LOG.trace("Scheduling execution of {}", message);
        self().tell(message, ActorRef.noSender());
    }

    @Override
    public final void onReceiveCommand(final Object message) throws Exception {
        final String messageType = message.getClass().getSimpleName();
        LOG.trace("Received message {}", messageType);

        if (message instanceof ExecuteInSelfMessage) {
            LOG.trace("Executing {}", message);
            ((ExecuteInSelfMessage) message).run();
        } else {
            handleCommand(message);
        }

        LOG.trace("Done handling message {}", messageType);
    }

    @Override
    public final void onReceiveRecover(final Object message) throws Exception {
        final String messageType = message.getClass().getSimpleName();
        LOG.trace("Received message {}", messageType);
        handleRecover(message);
        LOG.trace("Done handling message {}", messageType);
    }

    protected abstract void handleRecover(Object message) throws Exception;

    protected abstract void handleCommand(Object message) throws Exception;

    protected void ignoreMessage(final Object message) {
        LOG.debug("Unhandled message {} ", message);
    }

    protected void unknownMessage(final Object message) throws Exception {
        LOG.debug("Received unhandled message {}", message);
        unhandled(message);
    }
}
