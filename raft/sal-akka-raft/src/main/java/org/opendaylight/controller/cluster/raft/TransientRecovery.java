/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;

/**
 * An attempt to recover to non-persistent storage.
 *
 * @param <T> {@link State} type
 */
@NonNullByDefault
final class TransientRecovery<T extends State> extends Recovery<T> {
    TransientRecovery(final RaftActor actor, final RaftActorSnapshotCohort<T> snapshotCohort,
            final RaftActorRecoveryCohort recoveryCohort, final ConfigParams configParams) {
        super(actor, snapshotCohort, recoveryCohort, configParams);
    }

    @Override
    void discardSnapshottedEntries() {
        // No-op
    }
}
