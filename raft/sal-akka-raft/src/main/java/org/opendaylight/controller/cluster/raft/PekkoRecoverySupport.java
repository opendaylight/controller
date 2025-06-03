/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;

/**
 * Support class that handles persistence recovery for a RaftActor.
 *
 * @author Thomas Pantelis
 */
class PekkoRecoverySupport<T extends @NonNull State> {
    private final @NonNull RaftActor actor;
    private final @NonNull RaftActorRecoveryCohort recoveryCohort;
    private final @NonNull RaftActorSnapshotCohort<T> snapshotCohort;
    private final @NonNull ConfigParams configParams;

    @NonNullByDefault
    PekkoRecoverySupport(final RaftActor actor, final RaftActorSnapshotCohort<T> snapshotCohort,
            final RaftActorRecoveryCohort recoveryCohort, final ConfigParams configParams) {
        this.actor = requireNonNull(actor);
        this.snapshotCohort = requireNonNull(snapshotCohort);
        this.recoveryCohort = requireNonNull(recoveryCohort);
        this.configParams = requireNonNull(configParams);
    }

    @NonNull PekkoRecovery<T> recoverToPersistent() throws IOException {
        return new PekkoRecovery<>(actor, snapshotCohort, recoveryCohort, configParams);
    }

    @NonNull PekkoRecovery<T> recoverToTransient() throws IOException {
        return new PekkoRecovery.ToTransient<>(actor, snapshotCohort, recoveryCohort, configParams);
    }
}
