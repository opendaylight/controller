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
    private final CommitStage stage;

    CommitEntry(final CommitCohort cohort, final CommitStage.Ready stage) {
        this.cohort = requireNonNull(cohort);
        this.stage = requireNonNull(stage);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("cohort", cohort).add("stage", stage).toString();
    }
}
