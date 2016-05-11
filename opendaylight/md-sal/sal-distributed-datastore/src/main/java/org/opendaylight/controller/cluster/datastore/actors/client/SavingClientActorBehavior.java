/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <T> Frontend type
 *
 * @author Robert Varga
 */
final class SavingClientActorBehavior<T extends FrontendType> extends RecoveredClientActorBehavior<InitialClientActorContext<T>, T> {
    private static final Logger LOG = LoggerFactory.getLogger(SavingClientActorBehavior.class);
    private final ClientIdentifier<T> myId;

    SavingClientActorBehavior(final InitialClientActorContext<T> context, final ClientIdentifier<T> nextId) {
        super(context);
        this.myId = Preconditions.checkNotNull(nextId);
    }

    @Override
    AbstractClientActorBehavior<?> onReceiveCommand(final Object command) {
        if (command instanceof SaveSnapshotFailure) {
            LOG.error("{}: failed to persist state", persistenceId(), ((SaveSnapshotFailure) command).cause());
            return null;
        } else if (command instanceof SaveSnapshotSuccess) {
            context().unstash();
            return context().createBehavior(new ClientActorContext<>(self(), persistenceId(), myId));
        } else {
            LOG.debug("{}: stashing command {}", persistenceId(), command);
            context().stash();
            return this;
        }
    }
}