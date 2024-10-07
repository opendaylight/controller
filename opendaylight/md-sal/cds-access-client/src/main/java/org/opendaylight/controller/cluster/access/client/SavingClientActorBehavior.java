/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.persistence.DeleteSnapshotsFailure;
import org.apache.pekko.persistence.DeleteSnapshotsSuccess;
import org.apache.pekko.persistence.SaveSnapshotFailure;
import org.apache.pekko.persistence.SaveSnapshotSuccess;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transient behavior handling messages while the new generation is being persisted.
 */
final class SavingClientActorBehavior extends RecoveredClientActorBehavior<InitialClientActorContext> {
    private static final Logger LOG = LoggerFactory.getLogger(SavingClientActorBehavior.class);
    private final ClientIdentifier myId;

    SavingClientActorBehavior(final InitialClientActorContext context, final ClientIdentifier nextId) {
        super(context);
        myId = requireNonNull(nextId);
    }

    @Override
    AbstractClientActorBehavior<?> onReceiveCommand(final Object command) {
        switch (command) {
            case SaveSnapshotFailure saveFailure -> {
                LOG.error("{}: failed to persist state", persistenceId(), saveFailure.cause());
                return null;
            }
            case SaveSnapshotSuccess saved -> {
                LOG.debug("{}: got command: {}", persistenceId(), saved);
                context().deleteSnapshots(new SnapshotSelectionCriteria(scala.Long.MaxValue(),
                        saved.metadata().timestamp() - 1, 0L, 0L));
                return this;
            }
            case DeleteSnapshotsSuccess deleteSuccess -> {
                LOG.debug("{}: got command: {}", persistenceId(), deleteSuccess);
            }
            case DeleteSnapshotsFailure deleteFailure -> {
                // Not treating this as a fatal error.
                LOG.warn("{}: failed to delete prior snapshots", persistenceId(), deleteFailure.cause());
            }
            default -> {
                LOG.debug("{}: stashing command {}", persistenceId(), command);
                context().stash();
                return this;
            }
        }

        context().unstash();
        return context().createBehavior(myId);
    }
}
