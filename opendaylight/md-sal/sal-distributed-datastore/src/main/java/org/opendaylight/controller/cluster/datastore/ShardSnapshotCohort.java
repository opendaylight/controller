/*
 * Copyright (c) 2015, 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.raft.RaftActorSnapshotCohort;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.Support;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Participates in raft snapshotting on behalf of a Shard actor.
 *
 * @author Thomas Pantelis
 */
@NonNullByDefault
final class ShardSnapshotCohort implements RaftActorSnapshotCohort<ShardSnapshotState> {
    private static final Logger LOG = LoggerFactory.getLogger(ShardSnapshotCohort.class);

    private final ShardDataTree store;
    private final String memberName;

    ShardSnapshotCohort(final ShardDataTree store, final String memberName) {
        this.store = requireNonNull(store);
        this.memberName = requireNonNull(memberName);
    }

    @Override
    public Support<ShardSnapshotState> support() {
        return ShardSnapshotState.SUPPORT;
    }

    @Override
    public ShardSnapshotState takeSnapshot() {
        return new ShardSnapshotState(store.takeStateSnapshot());
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void applySnapshot(final ShardSnapshotState snapshotState) {
        final var snapshot = snapshotState.getSnapshot();

        // Since this will be done only on Recovery or when this actor is a Follower
        // we can safely commit everything in here. We not need to worry about event notifications
        // as they would have already been disabled on the follower

        LOG.info("{}: Applying snapshot", memberName);
        try {
            store.applySnapshot(snapshot);
        } catch (Exception e) {
            LOG.error("{}: Failed to apply snapshot {}", memberName, snapshot, e);
            return;
        }
        LOG.info("{}: Done applying snapshot", memberName);
    }
}
