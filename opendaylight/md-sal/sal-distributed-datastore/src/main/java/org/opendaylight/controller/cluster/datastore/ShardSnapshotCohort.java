/*
 * Copyright (c) 2015, 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.Optional;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4SafeDecompressor;
import net.jpountz.xxhash.XXHash32;
import net.jpountz.xxhash.XXHashFactory;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.actors.ShardSnapshotActor;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.raft.RaftActorSnapshotCohort;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;
import org.slf4j.Logger;

/**
 * Participates in raft snapshotting on behalf of a Shard actor.
 *
 * @author Thomas Pantelis
 */
abstract class ShardSnapshotCohort implements RaftActorSnapshotCohort {
    private static final class Plain extends ShardSnapshotCohort {
        Plain(final LocalHistoryIdentifier applyHistoryId, final ActorRef snapshotActor, final ShardDataTree store,
                final Logger log, final String logId) {
            super(applyHistoryId, snapshotActor, store, log, logId);
        }

        @Override
        InputStream createInputStream(final ByteSource snapshotBytes) throws IOException {
            return snapshotBytes.openStream();
        }
    }

    private static final class LZ4 extends ShardSnapshotCohort {
        private static final XXHashFactory HASH_FACTORY = XXHashFactory.fastestInstance();
        private static final LZ4Factory LZ4_FACTORY = LZ4Factory.fastestInstance();

        private final LZ4SafeDecompressor lz4Decompressor;
        private final XXHash32 hash32;

        LZ4(final LocalHistoryIdentifier applyHistoryId, final ActorRef snapshotActor, final ShardDataTree store,
                final Logger log, final String logId) {
            super(applyHistoryId, snapshotActor, store, log, logId);
            lz4Decompressor = LZ4_FACTORY.safeDecompressor();
            hash32 = HASH_FACTORY.hash32();
        }

        @Override
        InputStream createInputStream(final ByteSource snapshotBytes) throws IOException {
            return new LZ4FrameInputStream(snapshotBytes.openStream(), lz4Decompressor, hash32);
        }
    }

    private static final FrontendType SNAPSHOT_APPLY = FrontendType.forName("snapshot-apply");

    private final ActorRef snapshotActor;
    private final ShardDataTree store;
    private final String logId;
    private final Logger log;

    ShardSnapshotCohort(final LocalHistoryIdentifier applyHistoryId, final ActorRef snapshotActor,
            final ShardDataTree store, final Logger log, final String logId) {
        this.snapshotActor = requireNonNull(snapshotActor);
        this.store = requireNonNull(store);
        this.log = log;
        this.logId = logId;
    }

    static ShardSnapshotCohort create(final ActorContext actorContext, final MemberName memberName,
            final ShardDataTree store, final Logger log, final String logId, final DatastoreContext context) {
        final LocalHistoryIdentifier applyHistoryId = new LocalHistoryIdentifier(ClientIdentifier.create(
            FrontendIdentifier.create(memberName, SNAPSHOT_APPLY), 0), 0);
        final String snapshotActorName = "shard-" + memberName.getName() + ':' + "snapshot-read";
        boolean useLz4Compression = context.isUseLz4Compression();

        // Create a snapshot actor. This actor will act as a worker to offload snapshot serialization for all
        // requests.
        final ActorRef snapshotActor = actorContext.actorOf(ShardSnapshotActor.props(useLz4Compression),
                snapshotActorName);

        return useLz4Compression ? new LZ4(applyHistoryId, snapshotActor, store, log, logId)
                : new Plain(applyHistoryId, snapshotActor, store, log, logId);
    }

    @Override
    public void createSnapshot(final ActorRef actorRef, final Optional<OutputStream> installSnapshotStream) {
        // Forward the request to the snapshot actor
        final ShardDataTreeSnapshot snapshot = store.takeStateSnapshot();
        log.debug("{}: requesting serialization of snapshot {}", logId, snapshot);

        ShardSnapshotActor.requestSnapshot(snapshotActor, snapshot, installSnapshotStream, actorRef);
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void applySnapshot(final Snapshot.State snapshotState) {
        if (!(snapshotState instanceof ShardSnapshotState)) {
            log.debug("{}: applySnapshot ignoring snapshot: {}", logId, snapshotState);
        }

        final ShardDataTreeSnapshot snapshot = ((ShardSnapshotState)snapshotState).getSnapshot();

        // Since this will be done only on Recovery or when this actor is a Follower
        // we can safely commit everything in here. We not need to worry about event notifications
        // as they would have already been disabled on the follower

        log.info("{}: Applying snapshot", logId);

        try {
            store.applySnapshot(snapshot);
        } catch (Exception e) {
            log.error("{}: Failed to apply snapshot {}", logId, snapshot, e);
            return;
        }

        log.info("{}: Done applying snapshot", logId);
    }

    @Override
    public State deserializeSnapshot(final ByteSource snapshotBytes) throws IOException {
        try (ObjectInputStream in = getInputStream(snapshotBytes)) {
            return ShardDataTreeSnapshot.deserialize(in);
        }
    }

    private ObjectInputStream getInputStream(final ByteSource snapshotBytes) throws IOException {
        return new ObjectInputStream(createInputStream(snapshotBytes));
    }

    abstract InputStream createInputStream(ByteSource snapshotBytes) throws IOException;
}
