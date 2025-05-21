/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
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
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.RaftCallback;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
import org.opendaylight.controller.cluster.raft.spi.SnapshotFile;
import org.opendaylight.controller.cluster.raft.spi.SnapshotFileFormat;
import org.opendaylight.controller.cluster.raft.spi.StateMachineCommand;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.journal.FromByteBufMapper;
import org.opendaylight.raft.journal.SegmentedRaftJournal;
import org.opendaylight.raft.journal.StorageLevel;
import org.opendaylight.raft.journal.ToByteBufMapper;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.opendaylight.yangtools.concepts.WritableObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link EnabledRaftStorage} backed by Pekko Persistence of an {@link RaftActor}.
 */
// FIXME: remove this class once we have both Snapshots and Entries stored in files
final class PekkoRaftStorage extends EnabledRaftStorage {
    private static final Logger LOG = LoggerFactory.getLogger(PekkoRaftStorage.class);
    private static final HexFormat HF = HexFormat.of().withUpperCase();

    private static final String JOURNAL_NAME = "entries-v1";
    private static final String FILENAME_START_STR = "snapshot-";

    // 256KiB, which leads to reasonable buffers
    private static final int INLINE_ENTRY_SIZE = 256 * 1024;
    // We need to be able to store two entries and a descriptor
    //  private static final long MIN_TARGET_SIZE = INLINE_ENTRY_SIZE * 2 + SegmentDescriptor.BYTES;

    // Allocation of two bits, indicating the type of stored entry. These are top two bits, so as to allow combined use
    // with WritableObjects.writeLong(). This allows us to read the header byte and determine the dispatch accordingly.
    private static final byte TYPE_ENTRY         = (byte) 0x00;
    private static final byte TYPE_ENTRY_FILE    = (byte) 0x40;
    private static final byte TYPE_LAST_APPLIED  = (byte) 0x80;
    private static final byte TYPE_VOTING_CONFIG = (byte) 0xC0;
    private static final byte TYPE_MASK          = (byte) 0xC0;

    @NonNullByDefault
    private static final FromByteBufMapper<LoadedEntry> FROMBUF = (index, bytes) -> {
        try (var in = new ByteBufInputStream(bytes)) {
            final var header = in.readByte();
            final var type = header & TYPE_MASK;
            switch (type) {
                case TYPE_ENTRY -> {
                    final var hdr = WritableObjects.readLongHeader(in);
                    final var entryIndex = WritableObjects.readFirstLong(in, hdr);
                    final var entryTerm = WritableObjects.readSecondLong(in, hdr);

                    final StateMachineCommand command;
                    try (var ois = new ObjectInputStream(in)) {
                        try {
                            command = (StateMachineCommand) ois.readObject();
                        } catch (ClassNotFoundException e) {
                            throw new IOException("Cannot read command", e);
                        }
                    }
                    return new LoadedLogEntry(entryIndex, entryTerm, command);
                }
                case TYPE_LAST_APPLIED -> {
                    return new LoadedLastApplied(WritableObjects.readLongBody(in, header));
                }
                case TYPE_VOTING_CONFIG -> {
                    final var size = in.readInt();
                    final var builder = ImmutableList.<ServerInfo>builderWithExpectedSize(size);
                    for (int i = 0; i < size; ++i) {
                        builder.add(new ServerInfo(in.readUTF(), in.readBoolean()));
                    }
                    return new LoadedVotingConfig(new VotingConfig(builder.build()));
                }
                default -> {
                    throw new IllegalArgumentException("Unhandled type " + type);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    };

    @NonNullByDefault
    private static final @NonNull ToByteBufMapper<LogEntry> TOBUF_ENTRY = (obj, buf) -> {
        try (var out = new ByteBufOutputStream(buf)) {
            out.writeByte(TYPE_ENTRY);
            WritableObjects.writeLongs(out, obj.index(), obj.term());
            try (var oos = new ObjectOutputStream(out)) {
                oos.writeObject(obj.command().toSerialForm());
            }
        }
    };

    private static final @NonNull ToByteBufMapper<Long> TOBUF_LAST_APPLIED = (obj, buf) -> {
        try (var out = new ByteBufOutputStream(buf)) {
            WritableObjects.writeLong(out, obj, TYPE_LAST_APPLIED);
        }
    };

    @NonNullByDefault
    private static final @NonNull ToByteBufMapper<VotingConfig> TOBUF_VOTING_CONFIG = (obj, buf) -> {
        try (var out = new ByteBufOutputStream(buf)) {
            out.writeByte(TYPE_VOTING_CONFIG);
            final var si = obj.serverInfo();
            out.writeInt(si.size());
            for (var info : si) {
                out.writeUTF(info.peerId());
                out.writeBoolean(info.isVoting());
            }
        }
    };

    private final @NonNull RaftActor actor;
    private final @NonNull StorageLevel journalLevel;
    private final int journalSegmentSize;

    private SegmentedRaftJournal journal;

    @NonNullByDefault
    PekkoRaftStorage(final RaftActor actor, final Path directory, final CompressionType compression,
            final Configuration streamConfig) {
        super(actor.memberId(), actor, directory, compression, streamConfig);
        this.actor = requireNonNull(actor);
        journalSegmentSize = 128 * 1024 * 1024;
        journalLevel = StorageLevel.MAPPED;
    }

    // TODO: at least
    //   - creates the directory if not present
    //   - creates a 'lock' file and java.nio.channels.FileChannel.tryLock()s it
    //   - scans the directory to:
    //     - clean up any temporary files
    //     - determine nextSequence

    @Override
    protected void postStart() throws IOException {
        journal = SegmentedRaftJournal.builder()
            .withDirectory(directory)
            .withName(JOURNAL_NAME)
            .withMaxEntrySize(INLINE_ENTRY_SIZE)
            .withMaxSegmentSize(journalSegmentSize)
            .withStorageLevel(journalLevel)
            .build();
        LOG.info("{}: journal open: firstIndex={} lastIndex={}", memberId, journal.firstIndex(), journal.lastIndex());
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
        journal.close();
        journal = null;
        LOG.info("{}: journal closed", memberId);
    }

    @Override
    public SnapshotFile lastSnapshot() throws IOException {
        final var files = listFiles();
        if (files.isEmpty()) {
            LOG.debug("{}: no eligible files found", memberId);
            return null;
        }

        final var first = files.getLast();
        LOG.debug("{}: picked {} as the latest file", memberId, first);
        return first;
    }

    // Ordered by ascending timestamp, i.e. oldest snapshot first
    @NonNullByDefault
    private List<SnapshotFile> listFiles() throws IOException {
        if (!Files.exists(directory)) {
            LOG.debug("{}: directory {} does not exist", memberId, directory);
            return List.of();
        }

        final List<SnapshotFile> ret;
        try (var paths = Files.list(directory)) {
            ret = paths.map(this::pathToFile).filter(Objects::nonNull)
                .sorted(Comparator.comparing(SnapshotFile::timestamp))
                .toList();
        }
        LOG.trace("{}: recognized files: {}", memberId, ret);
        return ret;
    }

    private @Nullable SnapshotFile pathToFile(final @NonNull Path path) {
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
    public void persistEntry(final ReplicatedLogEntry entry) throws IOException {
        journal.writer().append(TOBUF_ENTRY, entry);
    }

    @Override
    public void persistVotingConfig(final VotingConfig votingConfig) throws IOException {
        journal.writer().append(TOBUF_VOTING_CONFIG, votingConfig);
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
        journal.writer().reset(fromIndex);
    }

    @Override
    public void markLastApplied(final long lastApplied) {
        try {
            journal.writer().append(TOBUF_LAST_APPLIED, lastApplied);
        } catch (IOException e) {
            LOG.error("{}: failed to mark last applied index", memberId, e);
            throw new UncheckedIOException(e);
        }
        LOG.debug("{}: update commit-index to {}", memberId, lastApplied);
    }

    @Override
    @NonNullByDefault
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
    @NonNullByDefault
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
    public void retainSnapshots(final Instant firstRetained) {
        final List<SnapshotFile> files;
        try {
            files = listFiles();
        } catch (IOException e) {
            LOG.warn("{}: failed to list snapshots, will retry next time", memberId, e);
            return;
        }

        for (var file : files) {
            if (firstRetained.compareTo(file.timestamp()) <= 0) {
                LOG.debug("{}: retaining snapshot {}", memberId, file);
                break;
            }

            try {
                // we should not have concurrent access, but it is okay if the file disappears independently
                Files.deleteIfExists(file.path());
            } catch (IOException e) {
                LOG.warn("{}: failed to delete snapshot {}, will retry next time", memberId, file, e);
                continue;
            }

            LOG.debug("{}: deleted snapshot {}", memberId, file);
        }
    }

    @Override
    public void deleteMessages(final long sequenceNumber) {
        actor.deleteMessages(sequenceNumber);
    }

    @Override
    public long lastSequenceNumber() {
        return journal.lastIndex();
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
