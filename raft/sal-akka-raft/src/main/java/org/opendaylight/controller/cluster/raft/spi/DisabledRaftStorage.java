/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.ToStorage;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;

/**
 * A {@link RaftStorage} backing non-persistent mode of {@link RaftActor} operation. It works with any actor which can
 * provide a {@link RaftStorageCompleter}.
 */
@NonNullByDefault
public final class DisabledRaftStorage extends RaftStorage implements ImmediateEntryStore {
    private static final class PersistEntryCallback extends RaftCallback<Instant> {
        private final RaftCallback<Long> callback;

        PersistEntryCallback(final RaftCallback<Long> callback) {
            this.callback = requireNonNull(callback);
        }

        @Override
        public void invoke(final @Nullable Exception failure, final @Nullable Instant success) {
            if (failure == null) {
                callback.invoke(null, 0L);
            } else {
                callback.invoke(failure, null);
            }
        }

        @Override
        protected ToStringHelper addToStringAttributes(final ToStringHelper helper) {
            return helper.add("callback", callback);
        }
    }

    public DisabledRaftStorage(final RaftStorageCompleter completer, final Path directory,
            final CompressionType compression, final Configuration streamConfig) {
        super(completer, directory, compression, streamConfig);
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
    public void startPersistEntry(final ReplicatedLogEntry entry, final RaftCallback<Long> callback) {
        if (entry.command() instanceof VotingConfig votingConfig) {
            saveSnapshot(new RaftSnapshot(votingConfig), EntryInfo.of(-1, -1), null,
                new PersistEntryCallback(callback));
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
        super.saveSnapshot(new RaftSnapshot(votingConfig), EntryInfo.of(-1, -1), null, timestamp);
    }
}
