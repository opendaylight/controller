/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import akka.persistence.RecoveryCompleted;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <T> Frontend type
 *
 * @author Robert Varga
 */
final class RecoveringClientActorBehavior<T extends FrontendType> extends AbstractClientActorBehavior<InitialClientActorContext<T>> {
    private static final Logger LOG = LoggerFactory.getLogger(RecoveringClientActorBehavior.class);
    private final FrontendIdentifier<T> currentFrontend;
    private ClientIdentifier<T> lastId = null;

    RecoveringClientActorBehavior(final InitialClientActorContext<T> context, final FrontendIdentifier<T> frontendId) {
        super(context);
        currentFrontend = Preconditions.checkNotNull(frontendId);
    }

    @Override
    AbstractClientActorBehavior<?> onReceiveCommand(final Object command) {
        throw new IllegalStateException("Frontend is recovering");
    }

    @SuppressWarnings("unchecked")
    @Override
    AbstractClientActorBehavior<?> onReceiveRecover(final Object recover) {
        if (recover instanceof RecoveryCompleted) {
            final ClientIdentifier<T> nextId;
            if (lastId != null) {
                Preconditions.checkState(currentFrontend.equals(lastId.getFrontendId()),
                    "Mismatched frontend identifier. Current: {} Saved: {}", currentFrontend,
                    lastId.getFrontendId());

                nextId = ClientIdentifier.create(currentFrontend, lastId.getGeneration() + 1);
            } else {
                nextId = ClientIdentifier.create(currentFrontend, 0);
            }

            LOG.debug("{}: persisting new identifier {}", persistenceId(), nextId);
            context().saveSnapshot(nextId);
            return new SavingClientActorBehavior<T>(context(), nextId);
        } else if (recover instanceof ClientIdentifier) {
            lastId = (ClientIdentifier<T>) recover;
            LOG.debug("{}: recovered identifier {}", lastId);
        } else {
            LOG.warn("{}: ignoring recovery message {}", recover);
        }

        return this;
    }
}