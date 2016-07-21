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
import akka.persistence.SnapshotSelectionCriteria;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert Varga
 */
final class SavingClientActorBehavior extends RecoveredClientActorBehavior<InitialClientActorContext> {
    private static final Logger LOG = LoggerFactory.getLogger(SavingClientActorBehavior.class);
    private final ClientIdentifier myId;

    SavingClientActorBehavior(final InitialClientActorContext context, final ClientIdentifier nextId) {
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

            SaveSnapshotSuccess saved = (SaveSnapshotSuccess)command;
            context().deleteSnapshots(new SnapshotSelectionCriteria(saved.metadata().sequenceNr(),
                    saved.metadata().timestamp() - 1, 0L, 0L));

            return context().createBehavior(myId);
        } else {
            LOG.debug("{}: stashing command {}", persistenceId(), command);
            context().stash();
            return this;
        }
    }
}