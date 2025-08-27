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
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;

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
    record Pending() implements CommitPhase {
        // FIXME: add state
    }

    /**
     * The transaction can start committing, i.e. it should transition to {@link CanCommit} as soon as practicable.
     */
    record NeedCanCommit(FutureCallback<Empty> callback) implements CommitPhase {
        public NeedCanCommit {
            requireNonNull(callback);
        }
    }

    /**
     * The transaction is in the {@code CanCommit phase}, gathering participant responses.
     */
    record CanCommit() implements CommitPhase {
        // FIXME: add state
    }

    /**
     * All of transaction's participants have indicated that transaction can be committed, i.e. it should transition
     * to {@link PreCommit} as soon as practicable.
     */
    record NeedPreCommit(FutureCallback<DataTreeCandidate> callback) implements CommitPhase {
        public NeedPreCommit {
            requireNonNull(callback);
        }
    }

    /**
     * The transaction is in the {@code PreCommit phase}, gathering participant responses.
     */
    record PreCommit() implements CommitPhase {
        // FIXME: add state
    }

    /**
     * The transaction is in the {@code DoCommit phase}.
     */
    record DoCommit(FutureCallback<UnsignedLong> callback) implements CommitPhase {
        public DoCommit {
            requireNonNull(callback);
        }
   }

    /**
     * Marker interface for all states that represent a concluded transaction, irrespective of whether or not it was
     * successful.
     */
    sealed interface Concluded extends CommitPhase {
        // Nothing Else
    }

    /**
     * The transaction has been aborted.
     */
    // FIXME: explain more
    record Aborted() implements Concluded {
        // FIXME: fill this out
    }

    /*
     * The transaction has been committed.
     */
    // FIXME: leader-side UTC Instant?
    record Committed() implements Concluded {
        // FIXME: add state
    }

    /**
     * The transaction has failed.
     */
    // FIXME: explain more
    record Failed() implements Concluded {
        // FIXME: add state
    }
}
