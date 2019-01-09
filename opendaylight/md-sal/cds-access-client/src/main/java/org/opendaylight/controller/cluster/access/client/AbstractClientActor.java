/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import com.google.common.annotations.Beta;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.common.actor.AbstractActorWithPersistenceFailureTracking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend actor which takes care of persisting generations and creates an appropriate ClientIdentifier.
 *
 * @author Robert Varga
 */
@Beta
public abstract class AbstractClientActor extends AbstractActorWithPersistenceFailureTracking {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractClientActor.class);
    private AbstractClientActorBehavior<?> currentBehavior;
    private volatile String persistenceId;

    protected AbstractClientActor(final FrontendIdentifier frontendId, final boolean backoffSupervised) {
        super(backoffSupervised);
        currentBehavior = new RecoveringClientActorBehavior(
                new InitialClientActorContext(this, frontendId.toPersistentId()), frontendId);
        persistenceId = currentBehavior.persistenceId();
    }

    @Override
    public final String persistenceId() {
        return persistenceId;
    }

    @Override
    public void postStop() throws Exception {
        if (currentBehavior != null) {
            currentBehavior.close();
        }

        super.postStop();
    }

    private void switchBehavior(final AbstractClientActorBehavior<?> nextBehavior) {
        if (!currentBehavior.equals(nextBehavior)) {
            if (nextBehavior == null) {
                LOG.debug("{}: shutting down", persistenceId());
                self().tell(PoisonPill.getInstance(), ActorRef.noSender());
            } else {
                LOG.debug("{}: switched from {} to {}", persistenceId(), currentBehavior, nextBehavior);
                persistenceId = nextBehavior.persistenceId();
            }

            currentBehavior.close();
            currentBehavior = nextBehavior;
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().matchAny(this::onReceiveCommand).build();
    }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder().matchAny(this::onReceiveRecover).build();
    }

    private void onReceiveCommand(final Object command) {
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

    private void onReceiveRecover(final Object recover) {
        switchBehavior(currentBehavior.onReceiveRecover(recover));
    }

    protected abstract ClientActorBehavior<?> initialBehavior(ClientActorContext context);

    protected abstract ClientActorConfig getClientActorConfig();
}
