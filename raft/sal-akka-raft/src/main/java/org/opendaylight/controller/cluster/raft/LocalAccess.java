/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.spi.TermInfoStore;

/**
 * The concept of {@link RaftActor}'s access to its local state.
 */
@NonNullByDefault
final class LocalAccess {
    private static final Path TERM_INFO_PROPS = Path.of("TermInfo.properties");
    private static final Path SNAPSHOTS = Path.of("snapshots");

    private final String memberId;
    private final TermInfoStore termInfoStore;
    private final Path stateDir;

    @VisibleForTesting
    LocalAccess(final String memberId, final Path stateDir, final TermInfoStore termInfoStore) {
        this.memberId = requireNonNull(memberId);
        this.stateDir = requireNonNull(stateDir);
        this.termInfoStore = requireNonNull(termInfoStore);
    }

    LocalAccess(final String memberId, final Path stateDir) {
        this(memberId, stateDir, new PropertiesTermInfoStore(memberId, stateDir.resolve(TERM_INFO_PROPS)));
    }

    String memberId() {
        return memberId;
    }

    Path snapshotDir() {
        return stateDir.resolve(SNAPSHOTS);
    }

    TermInfoStore termInfoStore() {
        return termInfoStore;
    }

    // FIXME: pretty much all of ReplicatedLog storage access, but not the indexing bits (at least not for now).
    //        Notable mentions:
    //          - receive leader-sourced snapshots into local store for use in Candidate, one at a time, allowing them
    //            to be atomically adopted to as local baseline snapshot
    //          - differentiated 'snapshot' and 'backup' operations?
    // FIXME: storage side of raft journal

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("memberId", memberId)
            .add("directory", stateDir)
            .add("termInfo", termInfoStore)
            .toString();
    }
}
