/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import java.util.SortedSet;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;

/**
 * A stage in transaction commit process. It mirrors the
 * <a href="https://en.wikipedia.org/wiki/Three-phase_commit_protocol">three-phase commit protocol</a>, but has also
 * some internal peculiarities.
 */
@NonNullByDefault
sealed interface CommitPhase {
    /**
     * Initial commit stage: the transaction is ready, but it cannot start committing just yet.
     */
    record Pending(
            DataTreeModification modification,
            // FIXME: remove userCohorts
            CompositeDataTreeCohort userCohorts,
            // FIXME: remove participatingShardNames
            @Nullable SortedSet<String> participatingShardNames) implements CommitPhase {
        public Pending {
            requireNonNull(modification);
            requireNonNull(userCohorts);
        }
    }

    /**
     * The transaction can start committing, i.e. it should transition to {@link CanCommit} as soon as practicable.
     */
    record NeedCanCommit(
            DataTreeModification modification,
            CompositeDataTreeCohort userCohorts,
            FutureCallback<Empty> callback,
            // FIXME: remove participatingShardNames
            @Nullable SortedSet<String> participatingShardNames) implements CommitPhase {
        public NeedCanCommit {
            requireNonNull(modification);
            requireNonNull(userCohorts);
            requireNonNull(callback);
        }
    }

    /**
     * The transaction is in the {@code CanCommit phase}, gathering participant responses.
     */
    record CanCommit(DataTreeModification modification, CompositeDataTreeCohort userCohorts) implements CommitPhase {
        public CanCommit {
            requireNonNull(modification);
            requireNonNull(userCohorts);
        }
    }

    /**
     * All of transaction's participants have indicated that transaction can be committed, i.e. it should transition
     * to {@link PreCommit} as soon as practicable.
     */
    record NeedPreCommit(
            DataTreeModification modification,
            CompositeDataTreeCohort userCohorts,
            FutureCallback<DataTreeCandidate> callback) implements CommitPhase {
        public NeedPreCommit {
            requireNonNull(modification);
            requireNonNull(userCohorts);
            requireNonNull(callback);
        }
    }


    /**
     * The transaction is in the {@code PreCommit phase}, gathering participant responses.
     */
    record PreCommit(
            DataTreeModification modification,
            CompositeDataTreeCohort userCohorts,
            DataTreeCandidateTip candidate) implements CommitPhase {
        public PreCommit {
            requireNonNull(modification);
            requireNonNull(userCohorts);
            requireNonNull(candidate);
        }
    }

    /**
     * The transaction is in the {@code DoCommit phase}.
     */
    record DoCommit(
            DataTreeModification modification,
            CompositeDataTreeCohort userCohorts,
            FutureCallback<UnsignedLong> callback) implements CommitPhase {
        public DoCommit {
            requireNonNull(modification);
            requireNonNull(userCohorts);
            requireNonNull(callback);
        }
    }

    /**
     * The transaction has been aborted.
     */
    // FIXME: explain more
    record Aborted() implements CommitPhase {
        // FIXME: fill this out
    }

    /*
     * The transaction has been committed.
     */
    // FIXME: leader-side UTC Instant?
    record Committed(UnsignedLong journalIndex) implements CommitPhase {
        public Committed {
            requireNonNull(journalIndex);
        }
    }

    /**
     * The transaction is known to be failing, but it has not yet been reported as failed.
     */
    record Failing(Exception cause) implements CommitPhase {
        public Failing {
            requireNonNull(cause);
        }
    }

    /**
     * The transaction has failed.
     */
    // FIXME: explain more
    record Failed(Exception cause) implements CommitPhase {
        public Failed {
            requireNonNull(cause);
        }
    }
}
