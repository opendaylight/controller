/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import java.util.SortedSet;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;

/**
 * A stage in transaction commit process. It closely mirrors the
 * <a href="https://en.wikipedia.org/wiki/Three-phase_commit_protocol">three-phase commit protocol</a>.
 */
@NonNullByDefault
sealed interface CommitStage {
    /**
     * Initial commit stage: the transaction is ready, but the process has not started yet.
     */
    record Ready(
            DataTreeModification modification,
            // FIXME: move userCohorts to CanCommitPending
            CompositeDataTreeCohort userCohorts,
            // FIXME: remove participatingShardNames
            @Nullable SortedSet<String> participatingShardNames) implements CommitStage {
        public Ready {
            requireNonNull(modification);
            requireNonNull(userCohorts);
        }
    }

    /**
     * The cohorts have been asked to prepare for commit, i.e. we are transitioning to the {@code CanCommit phase}.
     */
    record CanCommitPending() implements CommitStage {
        // FIXME: fill this out
    }

    /**
     * The transaction is in the {@code CanCommit phase}.
     */
    record CanCommit() implements CommitStage {
        // FIXME: fill this out
    }

    /**
     * The cohorts have been asked to pre-commit the transaction, i.e. we are transitioning to the
     * {@code PreCommit phase}.
     */
    record PreCommitPending() implements CommitStage {
        // FIXME: fill this out
    }


    /**
     * The transaction is in the {@code PreCommit phase}.
     */
    record PreCommit() implements CommitStage {
        // FIXME: fill this out
    }

    /**
     * The transaction is in the {@code DoCommit phase}.
     */
    record DoCommit() implements CommitStage {
        // FIXME: fill this out
    }

    /**
     * The transaction has been aborted.
     */
    // FIXME: explain more
    record Aborted() implements CommitStage {
        // FIXME: fill this out
    }

    /*
     * The transaction has been committed.
     */
    record Committed() implements CommitStage {
        // FIXME: fill this out
    }

    /**
     * The transaction has failed.
     */
    // FIXME: explain more
    record Failed() implements CommitStage {
        // FIXME: fill this out
    }
}
