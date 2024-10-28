/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.state;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.journal.AppendJournal;

/**
 * Methods exposed by {@link ElectedStateBehavior}. Broken out to allow separate implementation.
 */
@NonNullByDefault
public interface ElectedStateMethods {
    // note: can rely on local recovery
    default void onLocalDurable(final AppendJournal request) throws LeaderStateException {
        // defaults to no-op
    }

    // note: can rely on global recovery
    void onConsensusAchieved(AppendJournal request) throws LeaderStateException;
}