/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.persistence.DeleteSnapshotsFailure;
import akka.persistence.DeleteSnapshotsSuccess;
import akka.persistence.RecoveryCompleted;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.SnapshotOffer;
import akka.persistence.SnapshotSelectionCriteria;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transient behavior handling snapshot recovery and migration.
 */
final class RecoveringClientActorBehavior extends AbstractClientActorBehavior<InitialClientActorContext> {
    private static final Logger LOG = LoggerFactory.getLogger(RecoveringClientActorBehavior.class);

    private final FrontendIdentifier frontendId;

    private Object snapshot;

    RecoveringClientActorBehavior(final AbstractClientActor actor, final FrontendIdentifier frontendId) {
        super(new InitialClientActorContext(actor, frontendId));
        this.frontendId = frontendId;
    }

    @Override
    AbstractClientActorBehavior<?> onReceiveCommand(final Object command) {
        if (command instanceof SaveSnapshotSuccess saved) {
            context().deleteSnapshots(new SnapshotSelectionCriteria(scala.Long.MaxValue(),
                saved.metadata().timestamp() - 1, 0L, 0L));
            return this;
        } else if (command instanceof SaveSnapshotFailure saveFailure) {
            LOG.warn("{}: error saving tombstone", persistenceId(), saveFailure.cause());
        } else if (command instanceof DeleteSnapshotsSuccess) {
            LOG.debug("{}: snapshot migrated successfully", persistenceId());
        } else if (command instanceof DeleteSnapshotsFailure deleteFailure) {
            LOG.warn("{}: failed to delete prior snapshots", persistenceId(), deleteFailure.cause());
        } else {
            LOG.debug("{}: stashing command {}", persistenceId(), command);
            context().stash();
            return this;
        }
        context().unstash();
        return context().finishRecovery();
    }

    @Override
    AbstractClientActorBehavior<?> onReceiveRecover(final Object recover) {
        if (recover instanceof SnapshotOffer snapshotOffer) {
            snapshot = snapshotOffer.snapshot();
            return this;

        } else if (recover instanceof RecoveryCompleted) {
            if (snapshot instanceof ClientIdentifier clientId) {
                if (!frontendId.equals(clientId.getFrontendId())) {
                    // terminate if frontend id mismatches
                    LOG.error("{}: FrontendId mismatch for snapshot -> expected: {}, actual: {}",
                        persistenceId(), frontendId, clientId.getFrontendId());
                    return null;
                }
                // update generation and migrate to file storage
                final var newClientId = nextGenerationOf(clientId);
                context().updateFromSnapshot(newClientId);
                context().saveTombstone(newClientId);
                return this;

            } else if (snapshot instanceof PersistenceTombstone tombstone) {
                if (!frontendId.equals(tombstone.clientId().getFrontendId())) {
                    // terminate if frontend id mismatches
                    LOG.error("{}: FrontendId mismatch for snapshot -> expected: {}, actual: {}",
                        persistenceId(), frontendId, tombstone.clientId().getFrontendId());
                    return null;
                }
                // recover from tombstone if state file was deleted
                context().updateFromSnapshot(nextGenerationOf(tombstone.clientId()));
            }
        }
        return context().finishRecovery();
    }

    private static ClientIdentifier nextGenerationOf(final ClientIdentifier clientId) {
        return ClientIdentifier.create(clientId.getFrontendId(), clientId.getGeneration() + 1);
    }
}
