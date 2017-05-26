/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.persistence.RecoveryCompleted;
import akka.persistence.SnapshotOffer;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transient behavior handling messages during initial actor recovery.
 *
 * @author Robert Varga
 */
final class RecoveringClientActorBehavior extends AbstractClientActorBehavior<InitialClientActorContext> {
    private static final Logger LOG = LoggerFactory.getLogger(RecoveringClientActorBehavior.class);
    private final FrontendIdentifier currentFrontend;
    private ClientIdentifier lastId = null;

    RecoveringClientActorBehavior(final InitialClientActorContext context, final FrontendIdentifier frontendId) {
        super(context);
        currentFrontend = Preconditions.checkNotNull(frontendId);
    }

    @Override
    AbstractClientActorBehavior<?> onReceiveCommand(final Object command) {
        throw new IllegalStateException("Frontend is recovering");
    }

    @Override
    AbstractClientActorBehavior<?> onReceiveRecover(final Object recover) {
        if (recover instanceof RecoveryCompleted) {
            final ClientIdentifier nextId;
            if (lastId != null) {
                if (!currentFrontend.equals(lastId.getFrontendId())) {
                    LOG.error("{}: Mismatched frontend identifier, shutting down. Current: {} Saved: {}",
                        persistenceId(), currentFrontend, lastId.getFrontendId());
                    return null;
                }

                nextId = ClientIdentifier.create(currentFrontend, lastId.getGeneration() + 1);
            } else {
                nextId = ClientIdentifier.create(currentFrontend, 0);
            }

            LOG.debug("{}: persisting new identifier {}", persistenceId(), nextId);
            context().saveSnapshot(nextId);
            return new SavingClientActorBehavior(context(), nextId);
        } else if (recover instanceof SnapshotOffer) {
            lastId = (ClientIdentifier) ((SnapshotOffer)recover).snapshot();
            LOG.debug("{}: recovered identifier {}", persistenceId(), lastId);
        } else {
            LOG.warn("{}: ignoring recovery message {}", persistenceId(), recover);
        }

        return this;
    }
}