/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.FutureCallback;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeTip;

/**
 * A single entry in {@link ShardDataTree} commit queue.
 */
@NonNullByDefault
final class CommitEntry {
    /**
     * An individual stage in {@link CommitEntry} lifecycle.
     */
    sealed interface Stage {
        // Marker interface
    }

    /**
     * A ready transaction, which is pending a {@code canCommit} phase start.
     */
    record Ready(UserCohorts userCohorts) implements Stage {
        Ready {
            requireNonNull(userCohorts);
        }

        CanCommitPending toCanCommitPending(final FutureCallback<Empty> callback) {
            return new CanCommitPending(userCohorts, callback);
        }
    }

    /**
     * A transaction pending a {@code canCommit} phase result.
     */
    record CanCommitPending(UserCohorts userCohorts, FutureCallback<Empty> callback) implements Stage {
        CanCommitPending {
            requireNonNull(userCohorts);
            requireNonNull(callback);
        }

        CanCommitComplete toCanCommitComplete(final DataTreeTip tip) {
            return new CanCommitComplete(userCohorts, tip);
        }
    }

    /**
     * A transaction that has completed {@canCommit} successfully and is pending {@code preCommit} phase start.
     */
    record CanCommitComplete(UserCohorts userCohorts, DataTreeTip tip) implements Stage {
        CanCommitComplete {
            requireNonNull(userCohorts);
            requireNonNull(tip);
        }

        CanCommitComplete rebase(final DataTreeTip newTip) {
            return new CanCommitComplete(userCohorts, newTip);
        }

        PreCommitPending toPreCommitPending(final FutureCallback<DataTreeCandidate> callback) {
            return new PreCommitPending(userCohorts, tip, callback);
        }
    }

    /**
     * A transaction pending a {@preCommit} phase result.
     */
    record PreCommitPending(
            UserCohorts userCohorts,
            DataTreeTip tip,
            FutureCallback<DataTreeCandidate> callback) implements Stage {
        PreCommitPending {
            requireNonNull(userCohorts);
            requireNonNull(tip);
            requireNonNull(callback);
        }
    }

    //.FIXME: preCommitCompleted
    // FIXME: commitPending

    /**
     * A transaction which has committed successfully.
     */
    // FIXME: do we need this final stage?
    record Committed(long commitIndex) implements Stage {

    }

    /**
     * A transaction which is about to fail.
     */
    // FIXME: do we need this intermediate stage?
    record Failing(Exception cause) implements Stage {
        Failing {
            requireNonNull(cause);
        }
    }

    /**
     * A transaction which has failed.
     */
    // FIXME: do we need this final stage?
    record Failed(Exception cause) implements Stage {
        Failed {
            requireNonNull(cause);
        }
    }

    private final ReadWriteShardDataTreeTransaction transaction;

    private final Stage stage;
    private long lastAccess;

    CommitEntry(final ReadWriteShardDataTreeTransaction transaction, final Stage stage) {
        this.transaction = requireNonNull(transaction);
        this.stage = requireNonNull(stage);
    }

    TransactionIdentifier transactionId() {
        return transaction.getIdentifier();
    }

    long lastAccess() {
        return lastAccess;
    }

    void setLastAccess(final long newLastAccess) {
        lastAccess = newLastAccess;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("id", transactionId()).add("stage", stage).toString();
    }
}
