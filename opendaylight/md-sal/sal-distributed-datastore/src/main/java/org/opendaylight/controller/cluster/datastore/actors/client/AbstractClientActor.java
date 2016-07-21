/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.persistence.UntypedPersistentActor;
import com.google.common.annotations.Beta;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend actor which takes care of persisting generations and creates an appropriate ClientIdentifier.
 *
 * @author Robert Varga
 */
@Beta
public abstract class AbstractClientActor extends UntypedPersistentActor {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractClientActor.class);
    private AbstractClientActorBehavior<?> currentBehavior;

    protected AbstractClientActor(final FrontendIdentifier frontendId) {
        currentBehavior = new RecoveringClientActorBehavior(
                new InitialClientActorContext(this, frontendId.toPersistentId()), frontendId);
    }

    @Override
    public final String persistenceId() {
        return currentBehavior.persistenceId();
    }

    private void switchBehavior(final AbstractClientActorBehavior<?> nextBehavior) {
        if (!currentBehavior.equals(nextBehavior)) {
            if (nextBehavior == null) {
                LOG.debug("{}: shutting down", persistenceId());
                self().tell(PoisonPill.getInstance(), ActorRef.noSender());
            } else {
                LOG.debug("{}: switched from {} to {}", persistenceId(), currentBehavior, nextBehavior);
            }

            currentBehavior = nextBehavior;
        }
    }

    @Override
    public final void onReceiveCommand(final Object command) {
        if (command == null) {
            LOG.debug("{}: ignoring null command", persistenceId());
            return;
        }

        if (currentBehavior != null) {
            switchBehavior(currentBehavior.onReceiveCommand(command));
        } else {
            LOG.debug("{}: shutting down, ignoring command {}", persistenceId(), command);
        }
    }

    @Override
    public final void onReceiveRecover(final Object recover) {
        switchBehavior(currentBehavior.onReceiveRecover(recover));
    }

    protected abstract ClientActorBehavior initialBehavior(ClientActorContext context);
}
