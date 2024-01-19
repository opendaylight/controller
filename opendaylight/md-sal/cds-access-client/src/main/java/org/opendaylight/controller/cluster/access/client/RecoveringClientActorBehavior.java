/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static java.util.Objects.requireNonNull;

import akka.persistence.DeleteSnapshotsFailure;
import akka.persistence.DeleteSnapshotsSuccess;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.SnapshotProtocol.LoadSnapshotFailed;
import akka.persistence.SnapshotProtocol.LoadSnapshotResult;
import akka.persistence.SnapshotSelectionCriteria;
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
        currentFrontend = requireNonNull(frontendId);
    }

    @Override
    AbstractClientActorBehavior<?> onReceiveCommand(final Object command) {
        if (command instanceof LoadSnapshotResult loadResult) {
            if (loadResult.snapshot().nonEmpty()) {
                // snapshot exist, migrate if necessary
                final var snapshot = loadResult.snapshot().get();
                if (snapshot.snapshot() instanceof ClientIdentifier clientId) {
                    context().update(clientId);
                    context().saveTombstone(snapshot.metadata().sequenceNr() + 1, clientId);
                    return this;
                }
            }

        } else if (command instanceof LoadSnapshotFailed loadFailed) {
            LOG.error("{}: error loading snapshot", persistenceId(), loadFailed.cause());
            return null;

        } else if (command instanceof SaveSnapshotFailure saveFailure) {
            LOG.warn("{}: error saving tombstone", persistenceId(), saveFailure.cause());

        } else if (command instanceof SaveSnapshotSuccess saved) {
            context().deleteSnapshots(new SnapshotSelectionCriteria(scala.Long.MaxValue(),
                saved.metadata().timestamp() - 1, 0L, 0L));
            return this;

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

}
