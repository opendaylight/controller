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

/**
 * A single transaction undergoing the commit process.
 */
@NonNullByDefault
final class CommitEntry {
    private final CommitCohort cohort;

    private CommitStage stage;
    private long lastAccess;

    CommitEntry(final CommitCohort cohort, final CommitStage.Ready stage, final long lastAccess) {
        this.cohort = requireNonNull(cohort);
        this.stage = requireNonNull(stage);
        this.lastAccess = lastAccess;
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

    CommitStage stage() {
        return stage;
    }

    void setStage(final CommitStage newStage) {
        stage = requireNonNull(newStage);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("cohort", cohort)
            .add("stage", stage)
            .add("lastAccess", lastAccess)
            .toString();
    }
}
