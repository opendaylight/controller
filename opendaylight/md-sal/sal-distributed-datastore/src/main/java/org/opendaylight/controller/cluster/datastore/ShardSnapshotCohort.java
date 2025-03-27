/*
 * Copyright (c) 2015, 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import org.apache.pekko.actor.ActorContext;
import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.actors.ShardSnapshotActor;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.raft.RaftActorSnapshotCohort;
import org.opendaylight.raft.spi.InputOutputStreamFactory;
import org.opendaylight.raft.spi.Lz4BlockSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Participates in raft snapshotting on behalf of a Shard actor.
 *
 * @author Thomas Pantelis
 */
final class ShardSnapshotCohort implements RaftActorSnapshotCohort<ShardSnapshotState> {
    private static final Logger LOG = LoggerFactory.getLogger(ShardSnapshotCohort.class);
    private static final FrontendType SNAPSHOT_APPLY = FrontendType.forName("snapshot-apply");

    private final @NonNull InputOutputStreamFactory streamFactory;
    private final @NonNull ActorRef snapshotActor;
    private final @NonNull ShardDataTree store;
    private final @NonNull String memberName;

    ShardSnapshotCohort(final InputOutputStreamFactory streamFactory, final LocalHistoryIdentifier applyHistoryId,
            final ActorRef snapshotActor, final ShardDataTree store, final String memberName) {
        this.streamFactory = requireNonNull(streamFactory);
        this.snapshotActor = requireNonNull(snapshotActor);
        this.store = requireNonNull(store);
        this.memberName = requireNonNull(memberName);
    }

    static @NonNull ShardSnapshotCohort create(final ActorContext actorContext, final MemberName memberName,
            final ShardDataTree store, final String logId, final DatastoreContext context) {
        final var applyHistoryId = new LocalHistoryIdentifier(ClientIdentifier.create(
            FrontendIdentifier.create(memberName, SNAPSHOT_APPLY), 0), 0);
        final var streamFactory = context.isUseLz4Compression()
                ? InputOutputStreamFactory.lz4(Lz4BlockSize.LZ4_256KB) : InputOutputStreamFactory.simple();
        // Create a snapshot actor. This actor will act as a worker to offload snapshot serialization for all requests.
        final var snapshotActor = actorContext.actorOf(ShardSnapshotActor.props(streamFactory),
            "shard-" + memberName.getName() + ':' + "snapshot-read");
        return new ShardSnapshotCohort(streamFactory, applyHistoryId, snapshotActor, store, logId);
    }

    @Override
    public Class<ShardSnapshotState> stateClass() {
        return ShardSnapshotState.class;
    }

    @Override
    public ShardSnapshotState takeSnapshot() {
        return new ShardSnapshotState(store.takeStateSnapshot());
    }

    @Override
    public void createSnapshot(final ActorRef actorRef, final OutputStream installSnapshotStream) {
        // Forward the request to the snapshot actor
        final var snapshot = takeSnapshot();
        LOG.debug("{}: requesting serialization of snapshot {}", memberName, snapshot.getSnapshot());
        ShardSnapshotActor.requestSnapshot(snapshotActor, snapshot, installSnapshotStream, actorRef);
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

    @Override
    public ShardSnapshotState deserializeSnapshot(final ByteSource snapshotBytes) throws IOException {
        try (var in = new ObjectInputStream(streamFactory.createInputStream(snapshotBytes))) {
            return ShardDataTreeSnapshot.deserialize(in);
        }
    }
}
