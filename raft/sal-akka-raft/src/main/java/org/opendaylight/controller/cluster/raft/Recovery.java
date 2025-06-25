/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.persisted.MigratedSerializable;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.SnapshotStore;

/**
 * Base class for recovery implementations.
 *
 * @param <T> {@link State} type
 */
abstract sealed class Recovery<T extends @NonNull State> permits PekkoRecovery {
    final @NonNull RaftActor actor;
    final @NonNull RaftActorSnapshotCohort<T> snapshotCohort;
    final @NonNull RaftActorRecoveryCohort recoveryCohort;
    final @NonNull RecoveryLog recoveryLog;
    final int snapshotInterval;
    final int batchSize;

    private Stopwatch recoveryTimer;
    // FIXME: hide this field
    Stopwatch snapshotTimer;

    @NonNullByDefault
    Recovery(final RaftActor actor, final RaftActorSnapshotCohort<T> snapshotCohort,
            final RaftActorRecoveryCohort recoveryCohort, final ConfigParams configParams) {
        this.actor = requireNonNull(actor);
        this.snapshotCohort = requireNonNull(snapshotCohort);
        this.recoveryCohort = requireNonNull(recoveryCohort);
        snapshotInterval = configParams.getRecoverySnapshotIntervalSeconds();
        batchSize = configParams.getJournalRecoveryLogBatchSize();
        recoveryLog = new RecoveryLog(actor.memberId());
    }

    @NonNullByDefault
    final String memberId() {
        return actor.memberId();
    }

    @NonNullByDefault
    final SnapshotStore snapshotStore() {
        return actor.persistence().snapshotStore();
    }

    final void startRecoveryTimers() {
        if (recoveryTimer == null) {
            recoveryTimer = Stopwatch.createStarted();
        }
        if (snapshotTimer == null && snapshotInterval > 0) {
            snapshotTimer = Stopwatch.createStarted();
        }
    }

    @NonNullByDefault
    final String stopRecoveryTimers() {
        final String recoveryTime;
        if (recoveryTimer != null) {
            recoveryTime = "in " + recoveryTimer.stop();
            recoveryTimer = null;
        } else {
            recoveryTime = "";
        }

        if (snapshotTimer != null) {
            snapshotTimer.stop();
            snapshotTimer = null;
        }
        return recoveryTime;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("memberId", memberId()).toString();
    }

    @NonNullByDefault
    static final boolean isMigratedPayload(final LogEntry logEntry) {
        return isMigratedSerializable(logEntry.command().toSerialForm());
    }

    @NonNullByDefault
    static final boolean isMigratedSerializable(final Object message) {
        return message instanceof MigratedSerializable migrated && migrated.isMigrated();
    }
}
