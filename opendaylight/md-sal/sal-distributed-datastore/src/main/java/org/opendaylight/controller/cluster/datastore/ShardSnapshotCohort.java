/*
 * Copyright (c) 2015, 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.raft.RaftActorSnapshotCohort;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.Reader;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.Support;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.Writer;
import org.opendaylight.raft.spi.InputOutputStreamFactory;
import org.opendaylight.raft.spi.Lz4BlockSize;
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
    private static final FrontendType SNAPSHOT_APPLY = FrontendType.forName("snapshot-apply");

    private final Support<ShardSnapshotState> support;
    private final ShardDataTree store;
    private final String memberName;

    ShardSnapshotCohort(final InputOutputStreamFactory streamFactory, final LocalHistoryIdentifier applyHistoryId,
            final ShardDataTree store, final String memberName) {
        requireNonNull(streamFactory);
        this.store = requireNonNull(store);
        this.memberName = requireNonNull(memberName);

        support = new Support<>() {
            @Override
            public Class<ShardSnapshotState> snapshotType() {
                return ShardSnapshotState.class;
            }

            @Override
            public Reader<ShardSnapshotState> reader() {
                return source -> {
                    // FIXME: this guesswork is for LZ4 decompression. which should be handled elsewhere
                    try (var in = new ObjectInputStream(streamFactory.createInputStream(source))) {
                        return ShardDataTreeSnapshot.deserialize(in);
                    }
                };
            }

            @Override
            public Writer<ShardSnapshotState> writer() {
                return (snapshot, out) -> {
                    try (var oos = new ObjectOutputStream(out)) {
                        snapshot.getSnapshot().serialize(oos);
                    }
                };
            }
        };
    }

    static ShardSnapshotCohort create(final MemberName memberName, final ShardDataTree store, final String logId,
            final DatastoreContext context) {
        final var applyHistoryId = new LocalHistoryIdentifier(ClientIdentifier.create(
            FrontendIdentifier.create(memberName, SNAPSHOT_APPLY), 0), 0);
        final var streamFactory = context.isUseLz4Compression()
                ? InputOutputStreamFactory.lz4(Lz4BlockSize.LZ4_256KB) : InputOutputStreamFactory.simple();
        return new ShardSnapshotCohort(streamFactory, applyHistoryId, store, logId);
    }

    @Override
    public Support<ShardSnapshotState> support() {
        return support;
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
