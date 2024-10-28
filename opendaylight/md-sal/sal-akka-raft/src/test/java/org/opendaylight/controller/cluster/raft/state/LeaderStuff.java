/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.state;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.journal.AppendJournal;
import org.opendaylight.controller.cluster.raft.journal.Journal;

@NonNullByDefault
public non-sealed class LeaderStuff extends ActiveStuff {
    final Journal journal;

    LeaderStuff(final InactiveStuff prev, final Journal journal) {
        super(prev);
        this.journal = requireNonNull(journal);
    }

    void onConsensusAchieved(final AppendJournal request) throws LeaderStateException {
        // FIXME: implement
        throw new LeaderStateException("Not implemented");
    }
}
