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
import java.nio.file.Path;
import java.time.Instant;
import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;

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

    private final ActorRef actorRef;

    public DisabledRaftStorage(final String memberId, final ExecuteInSelfActor executeInSelf, final Path directory,
            final ActorRef actorRef, final CompressionType compression, final Configuration streamConfig) {
        super(memberId, executeInSelf, directory, compression, streamConfig);
        this.actorRef = requireNonNull(actorRef);
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
    public ExecuteInSelfActor actor() {
        return executeInSelf;
    }

    @Override
    public @Nullable SnapshotFile lastSnapshot() {
        // TODO: cache last encountered snapshot along with its lifecycle
        return null;
    }

    @Override
    public <T extends StateSnapshot> void saveSnapshot(final RaftSnapshot raftSnapshot, final EntryInfo lastIncluded,
            final T snapshot, final StateSnapshot.Writer<T> writer, final RaftCallback<Instant> callback) {
        final var timestamp = Instant.now();
        actor().executeInSelf(() -> callback.invoke(null, timestamp));
    }
}
