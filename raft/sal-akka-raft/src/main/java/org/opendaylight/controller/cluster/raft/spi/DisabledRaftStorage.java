/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.ToStorage;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;

/**
 * A {@link RaftStorage} backing non-persistent mode of {@link RaftActor} operation. It works with any actor which can
 * provide {@link ExecuteInSelfActor} services.
 */
@NonNullByDefault
public final class DisabledRaftStorage extends RaftStorage implements ImmediateEntryStore {
    public DisabledRaftStorage(final String memberId, final ExecuteInSelfActor executeInSelf, final Path directory,
            final CompressionType compression, final Configuration streamConfig) {
        super(memberId, executeInSelf, directory, compression, streamConfig);
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
    public void persistEntry(final ReplicatedLogEntry entry, final PersistCallback callback) {
        if (entry.command() instanceof VotingConfig votingConfig) {
            try {
                saveVotingConfig(votingConfig, Instant.now());
            } catch (IOException e) {
                callback.invoke(e, null);
                return;
            }
        }
        callback.invoke(null, 0L);
    }

    @Override
    public void startPersistEntry(final ReplicatedLogEntry entry, final PersistCallback callback) {
        if (entry.command() instanceof VotingConfig votingConfig) {
            saveSnapshot(new RaftSnapshot(votingConfig, List.of()), EntryInfo.of(-1, -1), null, (failure, success) -> {
                if (failure == null) {
                    callback.invoke(null, 0L);
                } else {
                    callback.invoke(failure, null);
                }
            });
        } else {
            ImmediateEntryStore.super.startPersistEntry(entry, callback);
        }
    }

    @Override
    public void saveSnapshot(final RaftSnapshot raftSnapshot, final EntryInfo lastIncluded,
            final @Nullable ToStorage<?> snapshot, final Instant timestamp) throws IOException {
        saveVotingConfig(raftSnapshot.votingConfig(), timestamp);
    }

    @VisibleForTesting
    public void saveVotingConfig(final @Nullable VotingConfig votingConfig, final Instant timestamp)
            throws IOException {
        // We always persist an empty snapshot
        super.saveSnapshot(new RaftSnapshot(votingConfig, List.of()), EntryInfo.of(-1, -1), null, timestamp);
    }
}
