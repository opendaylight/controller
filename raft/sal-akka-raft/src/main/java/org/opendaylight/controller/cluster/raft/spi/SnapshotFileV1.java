/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32C;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.ToStorage;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileStreamSource;
import org.opendaylight.raft.spi.RestrictedObjectStreams;
import org.opendaylight.raft.spi.SnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a version-1 {@link SnapshotFile}.
 */
@NonNullByDefault
final class SnapshotFileV1 implements SnapshotFile {
    private static final class CreatedFile implements Closeable {
        private final AtomicReference<@Nullable FileChannel> fc;

        CreatedFile(final FileChannel fc) {
            this.fc = new AtomicReference<>(requireNonNull(fc));
        }

        @Override
        public void close() throws IOException {
            final var local = fc.getAcquire();
            if (local != null && fc.compareAndExchangeRelease(local, null) == local) {
                try {
                    local.force(true);
                } finally {
                    local.close();
                }
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SnapshotFileV1.class);
    private static final HexFormat HF = HexFormat.of().withUpperCase();
    private static final Set<OpenOption> CREATE_OPTIONS = Set.of(
        StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, LinkOption.NOFOLLOW_LINKS);
    private static final Set<OpenOption> READ_OPTIONS = Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);

    // Snapshot file format:
    // offset  type     description
    //      0   u32     magic=0xE34C80B7
    //      4   u8      ClusterConfig format
    //                  - 0x00 = manual ClusterConfig \w DataOutput.writeUTF8
    //      5   u8      ReplicatedLogEntry format:
    //                  - 0x00 = plain List<ReplicatedLogEntry> \w Serializable
    //                  - 0x80 = LZ4 List<ReplicatedLogEntry> \w Serializable
    //                  Note: each entry has only 'term' and 'data', index is computed
    //      6   u8      StateSnapshot format:
    //                  - 0x00 = plain
    //                  - 0x80 = LZ4
    //      7   u8      reserved=0x00
    //      8   s64     last index
    //     16   s64     last term
    //     24   s64     Snapshot.State offset (SSO), <56 is invalid
    //     32   s64     file size, must be >= SSO
    //     40   s64     java.time.Instant.seconds
    //     48   s32     java.time.Instant.nanos
    //     52   u32     CRC32C of bytes [4..51]
    //     56   <var>   ClusterConfig
    //  <var>   <var>   List<ReplicatedLogEntry>
    //  <SSO>   <var>   Snapshot.State, zero-sized if not present
    private static final int HEADER_SIZE       = 56;
    private static final int MAGIC_BITS        = 0xE34C80B7;
    private static final byte COMPRESS_MASK    = (byte) 0xC0;
    // 0x30 reserved
    private static final byte SERDES_MASK      = (byte) 0x03;
    private static final byte COMPRESS_NONE    = (byte) 0x00;
    private static final byte COMPRESS_LZ4     = (byte) 0x80;
    // 0x40 reserved for compress method
    // 0xC0 reserved for compress method
    private static final byte SERDES_STATELESS = (byte) 0x00;
    // 0x01 reserved for serdes method
    // 0x02 reserved for serdes method
    // 0x03 reserved for serdes method

    private final Path path;
    private final EntryInfo lastIncluded;
    private final Instant timestamp;
    private final CompressionType entryCompress;
    private final CompressionType stateCompress;
    private final FileStreamSource serverStream;
    private final FileStreamSource stateStream;

    SnapshotFileV1(final Path path, final EntryInfo lastIncluded, final Instant timestamp,
            final CompressionType entryCompress, final CompressionType stateCompress,
            final long sso, final long limit) {
        this.path = requireNonNull(path);
        this.lastIncluded = requireNonNull(lastIncluded);
        this.timestamp = requireNonNull(timestamp);
        this.entryCompress = requireNonNull(entryCompress);
        this.stateCompress = requireNonNull(stateCompress);
        serverStream = new FileStreamSource(path, 0, sso);
        stateStream = new FileStreamSource(path, sso, limit);
    }

    static Closeable createNew(final Path file, final Instant timestamp, final EntryInfo lastIncluded,
            final @Nullable VotingConfig votingConfig, final CompressionType entryCompress,
            final List<LogEntry> unappliedEntries, final CompressionType stateCompress,
            final @Nullable ToStorage<?> state) throws IOException {
        final var entryFormat = computeFormat(entryCompress, "entry");
        final var stateFormat = computeFormat(stateCompress, "state");

        // Sanity check on unapplied entries
        if (!unappliedEntries.isEmpty()) {
            var prevIndex = lastIncluded.index();
            var prevTerm = lastIncluded.term();

            for (var entry : unappliedEntries) {
                final var expIndex = prevIndex + 1;
                final var index = entry.index();
                if (index != expIndex) {
                    throw new IOException("Expecting unapplied index " + HF.toHexDigits(expIndex) + " encountered "
                        + HF.toHexDigits(index));
                }
                final var term = entry.term();
                if (term < prevTerm) {
                    throw new IOException("Expecting unapplied term at least " + HF.toHexDigits(prevTerm)
                        + " encountered " + HF.toHexDigits(term));
                }
                prevIndex = index;
                prevTerm = term;
            }
        }

        final var fc = FileChannel.open(file, CREATE_OPTIONS);
        try {
            fc.position(HEADER_SIZE);

            final long sso;
            final long limit;
            try (var dos = new DataOutputStream(new UncloseableBufferedOutputStream(Channels.newOutputStream(fc)))) {
                // Emit server configuration if present
                if (votingConfig != null) {
                    final var si = votingConfig.serverInfo();
                    dos.writeInt(si.size());
                    for (var info : si) {
                        dos.writeUTF(info.peerId());
                        dos.writeBoolean(info.isVoting());
                    }
                } else {
                    dos.writeInt(-1);
                }

                // Emit unapplied entries, if any
                dos.writeInt(unappliedEntries.size());
                if (!unappliedEntries.isEmpty()) {
                    try (var oos = new ObjectOutputStream(entryCompress.encodeOutput(dos))) {
                        for (var entry : unappliedEntries) {
                            oos.writeLong(entry.term());
                            oos.writeObject(entry.command().toSerialForm());
                        }
                    }
                }

                // record SSO
                dos.flush();
                sso = fc.position();

                if (state != null) {
                    // emit state
                    try (var eos = stateCompress.encodeOutput(dos)) {
                        state.writeTo(eos);
                    }

                    // record limit
                    dos.flush();
                    limit = fc.position();
                    if (limit == sso) {
                        throw new IOException(state + " did not emit any bytes");
                    }
                } else {
                    limit = sso;
                }
            }

            // Construct header
            final var header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
            header.putInt(MAGIC_BITS)
                .put((byte) 0x00)
                .put(entryFormat)
                .put(stateFormat)
                .put((byte) 0x00)
                .putLong(lastIncluded.index())
                .putLong(lastIncluded.term())
                .putLong(sso)
                .putLong(limit)
                .putLong(timestamp.getEpochSecond())
                .putInt(timestamp.getNano());

            final var crc32 = new CRC32C();
            crc32.update(header.slice(4, 48));
            header.putInt((int) crc32.getValue());

            // Write header and sync
            fc.write(header.flip(), 0);
        } catch (IOException e) {
            try {
                fc.close();
            } catch (IOException ce) {
                e.addSuppressed(ce);
            }
            throw e;
        }
        return new CreatedFile(fc);
    }

    private static byte computeFormat(final CompressionType compress, final String which) throws IOException {
        return switch (compress) {
            case NONE -> COMPRESS_NONE | SERDES_STATELESS;
            case LZ4 -> COMPRESS_LZ4 | SERDES_STATELESS;
            default -> throw new IOException("Unsupported " + which + " compression " + compress);
        };
    }

    static SnapshotFileV1 open(final Path file) throws IOException {
        try (var fc = FileChannel.open(file, READ_OPTIONS)) {
            // TODO: Use a MemoryLayout when we have FFM?
            // Read the header
            final var header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
            fc.read(header, 0);
            return open(file, header.flip(), fc.size());
        } catch (BufferUnderflowException e) {
            throw new IOException("Internal problem opening file", e);
        }
    }

    private static SnapshotFileV1 open(final Path file, final ByteBuffer header, final long fileSize)
            throws IOException {
        // Check magic
        final var magic = header.getInt();
        if (magic != MAGIC_BITS) {
            throw new IOException(
                "Magic mismatch: expected " + HF.toHexDigits(MAGIC_BITS) + " found " + HF.toHexDigits(magic));
        }

        // +4
        final var configFormat = header.get();
        if (configFormat != 0) {
            throw new IOException("Unknown ClusterConfig format " + HF.toHexDigits(configFormat));
        }

        // +5
        final var entryFormat = header.get();
        final var entryCompress = selectCompression(entryFormat, "entry");
        selectSerdes(entryFormat, "entry");

        // +6
        final var stateFormat = header.get();
        final var stateCompress = selectCompression(stateFormat, "state");
        selectSerdes(stateFormat, "state");

        // +7
        final var reserved = header.get();
        if (reserved != 0x00) {
            LOG.warn("Ignoring reserved byte {}", HF.toHexDigits(reserved));
        }

        // +8
        // TODO: does 'index == -1' imply 'term == -1'?
        final var lastApplied = EntryInfo.of(header.getLong(), header.getLong());
        // +24
        final var sso = header.getLong();
        if (sso < header.limit()) {
            throw new IOException("Invalid user state offset " + sso);
        }

        // +32
        final var limit = header.getLong();
        if (fileSize < limit) {
            throw new IOException("Incompete file: expected " + limit + " observed " + fileSize);
        }
        if (fileSize > limit) {
            LOG.warn("File size mismatch: expected {} observed {}, Ignoring trailing {} bytes", limit, fileSize,
                fileSize - limit);
        }

        // +40
        final var tsSeconds = header.getLong();
        final var tsNanos = header.getInt();
        if (tsNanos < 0 || tsNanos > 999_999_999) {
            throw new IOException("Invalid timestamp nanoseconds " + tsNanos);
        }

        // +52
        final var checksum = header.getInt();
        verify(!header.hasRemaining());

        // compute and compare checksum
        final var crc32 = new CRC32C();
        crc32.update(header.slice(4, 48));
        final var computed = (int) crc32.getValue();
        if (computed != checksum) {
            throw new IOException(
                "Checksum mismatch: computed " + HF.toHexDigits(computed) + " recoded " + HF.toHexDigits(checksum));
        }

        return new SnapshotFileV1(file, lastApplied, Instant.ofEpochSecond(tsSeconds, tsNanos), entryCompress,
            stateCompress, sso, limit);
    }

    private static CompressionType selectCompression(final byte format, final String which) throws IOException {
        return switch (format & COMPRESS_MASK) {
            case COMPRESS_NONE -> CompressionType.NONE;
            case COMPRESS_LZ4 -> CompressionType.LZ4;
            default -> throw new IOException("Unhandled compression in " + which + " format " + HF.toHexDigits(format));
        };
    }

    private static void selectSerdes(final byte format, final String which) throws IOException {
        if ((format & SERDES_MASK) != SERDES_STATELESS) {
            throw new IOException("Unhandled serialization in " + which + " format " + HF.toHexDigits(format));
        }
    }

    @Override
    public Path path() {
        return path;
    }

    @Override
    public EntryInfo lastIncluded() {
        return lastIncluded;
    }

    @Override
    public Instant timestamp() {
        return timestamp;
    }

    @Override
    public RaftSnapshot readRaftSnapshot(final RestrictedObjectStreams objectStreams) throws IOException {
        try (var dis = serverStream.openDataInput()) {
            dis.skipNBytes(HEADER_SIZE);

            // Note: we do not compress VotingConfig on purpose, so as to ease debugging in case of any issues
            final VotingConfig votingConfig;
            final var siCount = dis.readInt();
            if (siCount >= 0) {
                final var siBuilder = ImmutableList.<ServerInfo>builderWithExpectedSize(siCount);
                for (int i = 0; i < siCount; i++) {
                    siBuilder.add(new ServerInfo(dis.readUTF(), dis.readBoolean()));
                }
                votingConfig = new VotingConfig(siBuilder.build());
            } else if (siCount == -1) {
                votingConfig = null;
            } else {
                throw new IOException("Invalid ServerInfo count " + siCount);
            }

            final var uaCount = dis.readInt();
            if (uaCount < 0) {
                throw new IOException("Invalid ReplicatedLogEntry count " + uaCount);
            }
            if (uaCount == 0) {
                return new RaftSnapshot(votingConfig);
            }

            final var uaBuilder = ImmutableList.<LogEntry>builderWithExpectedSize(uaCount);
            try (var ois = objectStreams.newObjectInputStream(entryCompress.decodeInput(dis))) {
                long prevIndex = lastIncluded.index();
                long prevTerm = lastIncluded.term();
                for (int i = 0; i < uaCount; i++) {
                    final var term = ois.readLong();
                    if (term < prevTerm) {
                        throw new IOException(
                            "Unexpected term " + HF.toHexDigits(term) + " after " + HF.toHexDigits(prevTerm));
                    }

                    final StateMachineCommand command;
                    try {
                        command = (StateMachineCommand) ois.readObject();
                    } catch (ClassCastException | ClassNotFoundException e) {
                        throw new IOException("Cannot deserialize payload in entry " + i, e);
                    }
                    if (command == null) {
                        throw new IOException("Unexpected null payload in entry " + i);
                    }
                    uaBuilder.add(new DefaultLogEntry(++prevIndex, term, command));
                    prevTerm = term;
                }
            }
            return new RaftSnapshot(votingConfig, uaBuilder.build());
        }
    }

    @Override
    public @Nullable SnapshotSource source() {
        return stateStream.size() == 0 ? null : stateCompress.nativeSource(stateStream);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("lastIncluded", lastIncluded)
            .add("timestamp", timestamp)
            .add("file", path)
            .add("size", stateStream.limit())
            .add("entryCompress", entryCompress)
            .add("stateCompress", stateCompress)
            .toString();
    }
}
