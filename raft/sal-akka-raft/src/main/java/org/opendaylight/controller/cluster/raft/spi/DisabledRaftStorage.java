/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.RaftActorSnapshotCohort;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.raft.spi.PlainSnapshotSource;
import org.opendaylight.raft.spi.SnapshotSource;

/**
 * A {@link RaftStorage} backing non-persistent mode of {@link RaftActor} operation. It works with any actor which can
 * provide {@link ExecuteInSelfActor} services.
 */
@NonNullByDefault
public final class DisabledRaftStorage extends RaftStorage implements ImmediateDataPersistenceProvider {
    // FIXME: this message should not be needed: saveSnapshot() should be taking a callback instead
    @Beta
    public static final class CommitSnapshot {
        public static final CommitSnapshot INSTANCE = new CommitSnapshot();

        private CommitSnapshot() {
            // Hidden on purpose
        }

        @Override
        public String toString() {
            return "CommitSnapshot";
        }
    }

    private final String memberId;
    private final ExecuteInSelfActor actor;
    private final ActorRef actorRef;

    public DisabledRaftStorage(final String memberId, final ExecuteInSelfActor actor, final ActorRef actorRef) {
        this.memberId = requireNonNull(memberId);
        this.actor = requireNonNull(actor);
        this.actorRef = requireNonNull(actorRef);
    }

    @Override
    protected String memberId() {
        return memberId;
    }

    @Override
    protected void postStart() {
        // No-op
    }

    @Override
    protected void preStop() {
        // No-op
    }

    @Override
    public @Nullable SnapshotSource tryLatestSnapshot() {
        // TODO: cache last encountered snapshot along with its lifecycle
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The way snapshotting works is:
     * <ol>
     *   <li>RaftActor calls {@link RaftActorSnapshotCohort#createSnapshot(ActorRef, java.io.OutputStream)}
     *   <li>the cohort sends a CaptureSnapshotReply and RaftActor then calls this method
     *   <li>When saveSnapshot is invoked on the akka-persistence API it uses the SnapshotStore to save
     *       the snapshot. The SnapshotStore sends SaveSnapshotSuccess or SaveSnapshotFailure. When the
     *       RaftActor gets SaveSnapshot success it commits the snapshot to the in-memory journal. This
     *       commitSnapshot is mimicking what is done in SaveSnapshotSuccess.
     * </ol>
     */
    @Override
    public void saveSnapshot(final Snapshot object) {
        // Make saving Snapshot successful
        // Committing the snapshot here would end up calling commit in the creating state which would
        // be a state violation. That's why now we send a message to commit the snapshot.
        actorRef.tell(CommitSnapshot.INSTANCE, ActorRef.noSender());
    }

    @Override
    public void saveSnapshot(final SnapshotWriter writer,
            final BiConsumer<@Nullable SnapshotSource, @Nullable ? super Throwable> callback) {
        Futures.addCallback(submit((Callable<SnapshotSource>) () -> {
            // FIXME: actual temp file, etc.
            final byte[] bytes;
            try (var baos = new ByteArrayOutputStream()) {
                bytes = baos.toByteArray();
            }
            return (PlainSnapshotSource) () -> new ByteArrayInputStream(bytes);
        }), new FutureCallback<>() {
            @Override
            public void onSuccess(final @Nullable SnapshotSource result) {
                callback.accept(result, null);
            }

            @Override
            public void onFailure(final @Nullable Throwable failure) {
                callback.accept(null, failure);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public void executeInSelf(final Runnable runnable) {
        actor.executeInSelf(runnable);
    }
}
