/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.pekko.persistence.DeleteMessagesSuccess;
import org.apache.pekko.persistence.DeleteSnapshotsSuccess;
import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.RaftCallback;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
import org.opendaylight.controller.cluster.raft.spi.SnapshotFile;
import org.opendaylight.controller.cluster.raft.spi.SnapshotFileFormat;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link EnabledRaftStorage} backed by Pekko Persistence of an {@link RaftActor}.
 */
// FIXME: remove this class once we have both Snapshots and Entries stored in files
@NonNullByDefault
final class PekkoRaftStorage extends EnabledRaftStorage {
    private static final Logger LOG = LoggerFactory.getLogger(PekkoRaftStorage.class);
    private static final HexFormat HF = HexFormat.of().withUpperCase();

    private static final String FILENAME_START_STR = "snapshot-";

    private final RaftActor actor;

    PekkoRaftStorage(final RaftActor actor, final Path directory, final CompressionType compression,
            final Configuration streamConfig) {
        super(actor.memberId(), actor, directory, compression, streamConfig);
        this.actor = requireNonNull(actor);
    }

    // TODO: at least
    //   - creates the directory if not present
    //   - creates a 'lock' file and java.nio.channels.FileChannel.tryLock()s it
    //   - scans the directory to:
    //     - clean up any temporary files
    //     - determine nextSequence

    @Override
    protected void postStart() {
        // No-op
    }

    // FIXME:  and more: more things:
    //   - terminates any incomplete operations, reporting a CancellationException to them
    //   - unlocks the file
    // - stop() that:
    // - a lava.lang.ref.Clearner which does the same as stop()
    // For scalability this locking should really be done on top-level stateDir so we have one file open for all shards,
    // not one file per shard.

    @Override
    protected void preStop() {
        // No-op
    }

    @Override
    public @Nullable SnapshotFile lastSnapshot() throws IOException {
        final var files = listFiles();
        if (files.isEmpty()) {
            LOG.debug("{}: no eligible files found", memberId);
            return null;
        }

        final var first = files.getFirst();
        LOG.debug("{}: picked {} as the latest file", memberId, first);
        return first;
    }

    private List<SnapshotFile> listFiles() throws IOException {
        if (!Files.exists(directory)) {
            LOG.debug("{}: directory {} does not exist", memberId, directory);
            return List.of();
        }

        final List<SnapshotFile> ret;
        try (var paths = Files.list(directory)) {
            ret = paths.map(this::pathToFile).filter(Objects::nonNull)
                .sorted(Comparator.comparing(SnapshotFile::timestamp).reversed())
                .toList();
        }
        LOG.trace("{}: recognized files: {}", memberId, ret);
        return ret;
    }

    private @Nullable SnapshotFile pathToFile(final Path path) {
        if (!Files.isRegularFile(path)) {
            LOG.debug("{}: skipping non-file {}", memberId, path);
            return null;
        }
        if (!Files.isReadable(path)) {
            LOG.debug("{}: skipping unreadable file {}", memberId, path);
            return null;
        }
        final var name = path.getFileName().toString();
        if (!name.startsWith(FILENAME_START_STR)) {
            LOG.debug("{}: skipping unrecognized file {}", memberId, path);
            return null;
        }
        final var format = SnapshotFileFormat.forFileName(name);
        if (format == null) {
            LOG.debug("{}: skipping unhandled file {}", memberId, path);
            return null;
        }
        LOG.debug("{}: selected {} to handle file {}", memberId, format, path);

        try {
            return format.open(path);
        } catch (IOException e) {
            LOG.warn("{}: cannot open {}, skipping", memberId, path, e);
            return null;
        }
    }

    @Override
    public void persistEntry(final ReplicatedLogEntry entry, final Consumer<ReplicatedLogEntry> callback) {
        actor.persist(entry, callback);
    }

    @Override
    public void persistVotingConfig(final VotingConfig votingConfig, final Consumer<VotingConfig> callback) {
        actor.persist(votingConfig, callback);
    }

    @Override
    public void startPersistEntry(final ReplicatedLogEntry entry, final Consumer<ReplicatedLogEntry> callback) {
        actor.persistAsync(entry, callback);
    }

    @Override
    public void startPersistVotingConfig(final VotingConfig votingConfig, final Consumer<VotingConfig> callback) {
        actor.persistAsync(votingConfig, callback);
    }

    @Override
    public void deleteEntries(final long fromIndex) {
        actor.deleteEntries(fromIndex);
    }

    @Override
    public void markLastApplied(final long lastApplied) {
        actor.markLastApplied(lastApplied);
    }

    @Override
    public <T extends StateSnapshot> void saveSnapshot(final RaftSnapshot raftSnapshot, final EntryInfo lastIncluded,
            final @Nullable T snapshot, final StateSnapshot.Writer<T> writer, final RaftCallback<Instant> callback) {
        requireNonNull(raftSnapshot);
        requireNonNull(lastIncluded);
        requireNonNull(writer);

        submitTask(new CancellableTask<>(callback) {
            @Override
            protected Instant compute() throws IOException {
                final var timestamp = Instant.now();
                saveSnapshot(raftSnapshot, lastIncluded, snapshot, writer, timestamp);
                return timestamp;
            }
        });
    }

    @Override
    public <T extends StateSnapshot> void saveSnapshot(final RaftSnapshot raftSnapshot, final EntryInfo lastIncluded,
            final @Nullable T snapshot, final StateSnapshot.Writer<T> writer, final Instant timestamp)
                throws IOException {
        final var format = SnapshotFileFormat.latest();
        final var baseName = new StringBuilder()
            .append(FILENAME_START_STR)
            .append(HF.toHexDigits(timestamp.getEpochSecond()))
            .append('-')
            .append(HF.toHexDigits(timestamp.getNano()));

        final var tmpPath = directory.resolve(baseName + ".tmp");
        final var filePath = directory.resolve(baseName + format.extension());

        LOG.debug("{}: starting snapshot writeout to {}", memberId, tmpPath);

        try {
            format.createNew(tmpPath, timestamp, lastIncluded, raftSnapshot.votingConfig(), compression,
                raftSnapshot.unappliedEntries(), compression, writer, snapshot).close();
            Files.move(tmpPath, filePath, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.deleteIfExists(tmpPath);
            throw e;
        }

        LOG.debug("{}: finished snapshot writeout to {}", memberId, filePath);
    }

    @Override
    public void deleteSnapshots(final long maxTimestamp) {
        actor.deleteSnapshots(maxTimestamp);
    }

    @Override
    public void deleteMessages(final long sequenceNumber) {
        actor.deleteMessages(sequenceNumber);
    }

    @Override
    public long lastSequenceNumber() {
        return actor.lastSequenceNr();
    }

    @Override
    public boolean handleJournalResponse(final JournalProtocol.Response response) {
        return response instanceof DeleteMessagesSuccess;
    }

    @Override
    public boolean handleSnapshotResponse(final SnapshotProtocol.Response response) {
        return response instanceof DeleteSnapshotsSuccess;
    }
}
