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
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * A single transaction undergoing the commit process. It is composed of a commit {@link #cohort()} and a commit
 * {@link #stage()}, along with {@link #lastAccess()} time.
 */
@NonNullByDefault
final class CommitEntry {
    private final CommitCohort cohort;

    private CommitPhase phase;
    private long lastAccess;

    CommitEntry(final CommitCohort cohort, final CommitPhase.Pending phase, final long lastAccess) {
        this.cohort = requireNonNull(cohort);
        this.phase = requireNonNull(phase);
        this.lastAccess = lastAccess;
    }

    TransactionIdentifier transactionId() {
        return cohort.transactionId();
    }

    CommitCohort cohort() {
        return cohort;
    }

    long lastAccess() {
        return lastAccess;
    }

    void setLastAccess(final long newLastAccess) {
        lastAccess = newLastAccess;
    }

    CommitPhase phase() {
        return phase;
    }

    void setStage(final CommitPhase newStage) {
        phase = requireNonNull(newStage);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("cohort", cohort)
            .add("phase", phase)
            .add("lastAccess", lastAccess)
            .toString();
    }
}
