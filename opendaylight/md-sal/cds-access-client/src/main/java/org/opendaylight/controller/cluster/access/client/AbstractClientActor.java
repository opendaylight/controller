/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import com.google.common.annotations.VisibleForTesting;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend actor which takes care of persisting generations and creates an appropriate ClientIdentifier.
 */
// FIXME: convert to akka.actor.typed(.javadsl.Behaviours)
public abstract class AbstractClientActor extends AbstractActorWithStash {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractClientActor.class);
    private AbstractClientActorBehavior<?> currentBehavior;
    private boolean recoveryStageCompleted;

    protected AbstractClientActor(final FrontendIdentifier frontendId) {
        currentBehavior = new RecoveringClientActorBehavior(
                new InitialClientActorContext(this, frontendId), frontendId);
    }

    public final String persistenceId() {
        return currentBehavior.persistenceId();
    }

    @Override
    public void preStart() throws Exception {
        if (currentBehavior != null && currentBehavior.context() instanceof InitialClientActorContext initialContext) {
            // due to current actor no longer served by akka persistence (which initiated recovery flow
            // with SnapshotOffer message), getting existing snapshot requires explicit LoadSnapshot call
            // to SnapshotStore, so existing snapshot data can be migrated to local file storage
            initialContext.startSnapshotMigration();
        }
        super.preStart();
    }

    @Override
    public void postStop() throws Exception {
        if (currentBehavior != null) {
            currentBehavior.close();
        }

        super.postStop();
    }

    @VisibleForTesting
    boolean recoveryStageCompleted() {
        return recoveryStageCompleted;
    }

    private void switchBehavior(final AbstractClientActorBehavior<?> nextBehavior) {
        if (!currentBehavior.equals(nextBehavior)) {
            if (currentBehavior instanceof RecoveringClientActorBehavior) {
                recoveryStageCompleted = true;
            }

            if (nextBehavior == null) {
                LOG.debug("{}: shutting down", persistenceId());
                self().tell(PoisonPill.getInstance(), ActorRef.noSender());
            } else {
                LOG.debug("{}: switched from {} to {}", persistenceId(), currentBehavior, nextBehavior);
            }

            currentBehavior.close();
            currentBehavior = nextBehavior;
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().matchAny(this::onReceiveCommand).build();
    }

    private void onReceiveCommand(final Object command) {
        LOG.info("command: {}", command.getClass());

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

    protected abstract ClientActorBehavior<?> initialBehavior(ClientActorContext context);

    protected abstract ClientActorConfig getClientActorConfig();
}
