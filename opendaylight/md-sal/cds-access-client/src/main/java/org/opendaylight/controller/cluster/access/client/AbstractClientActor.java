/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.persistence.AbstractPersistentActor;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend actor which takes care of persisting generations and creates an appropriate ClientIdentifier.
 */
public abstract class AbstractClientActor extends AbstractPersistentActor {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractClientActor.class);
    private static final Path STATE_PATH = Path.of("state");

    private final @NonNull String persistenceId;

    private AbstractClientActorBehavior<?> currentBehavior;

    protected AbstractClientActor(final FrontendIdentifier frontendId) {
        this(STATE_PATH, frontendId);
    }

    @VisibleForTesting
    AbstractClientActor(final Path statePath, final FrontendIdentifier frontendId) {
        persistenceId = frontendId.toPersistentId();
        currentBehavior = new RecoveringClientActorBehavior(statePath, this, persistenceId, frontendId);
    }

    @Override
    public final String persistenceId() {
        return persistenceId;
    }

    @Override
    public final void preStart() throws Exception {
        super.preStart();
    }

    @Override
    public final void postStop() throws Exception {
        if (currentBehavior != null) {
            currentBehavior.close();
            currentBehavior = null;
        }
        super.postStop();
    }

    private void switchBehavior(final AbstractClientActorBehavior<?> nextBehavior) {
        if (!currentBehavior.equals(nextBehavior)) {
            if (nextBehavior == null) {
                LOG.debug("{}: shutting down", persistenceId);
                self().tell(PoisonPill.getInstance(), ActorRef.noSender());
            } else {
                LOG.debug("{}: switched from {} to {}", persistenceId, currentBehavior, nextBehavior);
            }

            currentBehavior.close();
            currentBehavior = nextBehavior;
        }
    }

    @Override
    public final Receive createReceive() {
        return receiveBuilder().matchAny(this::onReceiveCommand).build();
    }

    @Override
    public final Receive createReceiveRecover() {
        return receiveBuilder().matchAny(this::onReceiveRecover).build();
    }

    private void onReceiveCommand(final Object command) {
        if (command == null) {
            LOG.debug("{}: ignoring null command", persistenceId);
            return;
        }

        if (currentBehavior != null) {
            switchBehavior(currentBehavior.onReceiveCommand(command));
        } else {
            LOG.debug("{}: shutting down, ignoring command {}", persistenceId, command);
        }
    }

    private void onReceiveRecover(final Object recover) {
        switchBehavior(currentBehavior.onReceiveRecover(recover));
    }

    protected abstract ClientActorBehavior<?> initialBehavior(ClientActorContext context);

    protected abstract ClientActorConfig getClientActorConfig();
}
