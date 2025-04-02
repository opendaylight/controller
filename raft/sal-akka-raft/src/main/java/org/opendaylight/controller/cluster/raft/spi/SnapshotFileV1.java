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
import com.google.common.io.ByteStreams;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HexFormat;
import java.util.zip.CRC32C;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.CompressionSupport;
import org.opendaylight.raft.spi.SnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a version-1 {@link SnapshotFile}.
 */
@NonNullByDefault
final class SnapshotFileV1 implements SnapshotFile {
    private static final Logger LOG = LoggerFactory.getLogger(SnapshotFileV1.class);
    private static final HexFormat HF = HexFormat.of().withUpperCase();

    // Snapshot file format:
    // offset  type     description
    //      0   u32     magic=0xE34C80B7
    //      4   u8      ClusterConfig format
    //                  - 0x00 = manual ClusterConfig \w DataOutput.writeUTF8
    //      5   u8      ReplicatedLogEntry format:
    //                  - 0x00 = plain List<ReplicatedLogEntry> \w Serializable
    //                  - 0x80 = LZ4 List<ReplicatedLogEntry> \w Serializable
    //                  Note: each entry has only 'term' and 'data', index is computed
    //      6   u8      Snapshot.State format:
    //                  - 0x00 = plain \w Serializable
    //                  - 0x80 = LZ4 \w Serializable
    //      7   u8      reserved=0x00
    //      8   s64     last index
    //     16   s64     last term
    //     24   s64     Snapshot.State offset (SSO), <56 is invalid
    //     32   s64     file size, must be >= SSO
    //     40   s64     java.time.Instant.seconds
    //     48   s32     java.time.Instant.nanos
    //     52   u32     CRC32C
    //     56   <var>   ClusterConfig
    //  <var>   <var>   List<ReplicatedLogEntry>
    //  <SSO>   <var>   Snapshot.State
    private static final int HEADER_SIZE = 56;
    static final int MAGIC_BITS = 0xE34C80B7;
    private static final byte COMPRESS_MASK = (byte) 0xC0;
    // 0x30 reserved
    private static final byte SERDES_MASK   = (byte) 0x03;
    private static final int COMPRESS_NONE = 0x00;
    private static final int COMPRESS_LZ4  = 0x80;
    // 0x40 reserved for compress method
    // 0xC0 reserved for compress method
    private static final int SERDES_JAVA   = 0x00;
    // 0x01 reserved for serdes method
    // 0x02 reserved for serdes method
    // 0x03 reserved for serdes method

    private final Path file;
    private final EntryInfo lastIncluded;
    private final Instant timestamp;
    private final CompressionSupport entryCompress;
    private final CompressionSupport stateCompress;
    private final long sso;
    private final long limit;

    SnapshotFileV1(final Path file, final EntryInfo lastIncluded, final Instant timestamp,
            final CompressionSupport entryCompress, final CompressionSupport stateCompress,
            final long sso, final long limit) {
        this.file = requireNonNull(file);
        this.lastIncluded = requireNonNull(lastIncluded);
        this.timestamp = requireNonNull(timestamp);
        this.entryCompress = requireNonNull(entryCompress);
        this.stateCompress = requireNonNull(stateCompress);
        this.sso = sso;
        this.limit = limit;
    }

    static SnapshotFile open(final Path file) throws IOException {
        try (var fc = FileChannel.open(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            // TODO: Use a MemoryLayout when we have FFM?
            // Read the header
            final var header = ByteBuffer.allocate(SnapshotFileV1.HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
            fc.read(header, 0);
            return open(file, header.flip(), fc.size());
        } catch (BufferUnderflowException e) {
            throw new IOException("Internal problem opening file", e);
        }
    }

    private static SnapshotFile open(final Path file, final ByteBuffer header, final long fileSize) throws IOException {
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
        final var entryCompress = switch (entryFormat & COMPRESS_MASK) {
            case COMPRESS_NONE -> CompressionSupport.NONE;
            case COMPRESS_LZ4 -> CompressionSupport.LZ4;
            default -> throw new IOException(
                "Unhandled compression in entry format " + HF.toHexDigits(entryFormat));
        };
        if ((entryFormat & SERDES_MASK) != SERDES_JAVA) {
            throw new IOException("Unhandled serialization in entry format " + HF.toHexDigits(entryFormat));
        }

        // +6
        final var stateFormat = header.get();
        final var stateCompress = switch (stateFormat & COMPRESS_MASK) {
            case COMPRESS_NONE -> CompressionSupport.NONE;
            case COMPRESS_LZ4 -> CompressionSupport.LZ4;
            default -> throw new IOException(
                "Unhandled compression in state format " + HF.toHexDigits(stateFormat));
        };
        if ((stateFormat & SERDES_MASK) != SERDES_JAVA) {
            throw new IOException("Unhandled serialization in entry format " + HF.toHexDigits(stateFormat));
        }

        // +7
        final var reserved = header.get();
        if (reserved != 0x00) {
            LOG.warn("Ignoring reserved byte " + HF.toHexDigits(reserved));
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

    @Override
    public EntryInfo lastIncluded() {
        return lastIncluded;
    }

    @Override
    public Instant timestamp() {
        return timestamp;
    }

    @Override
    public ServerState readServerState() throws IOException {
        try (var dis = new DataInputStream(
                ByteStreams.limit(Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS), sso))) {
            dis.skipNBytes(HEADER_SIZE);

            final var siCount = dis.readInt();
            if (siCount < 0) {
                throw new IOException("Invalid ServerInfo count " + siCount);
            }

            final var siBuilder = ImmutableList.<ServerInfo>builderWithExpectedSize(siCount);
            for (int i = 0; i < siCount; i++) {
                siBuilder.add(new ServerInfo(dis.readUTF(), dis.readBoolean()));
            }
            final var clusterConfig = new ClusterConfig(siBuilder.build());

            final var uaCount = dis.readInt();
            if (uaCount < 0) {
                throw new IOException("Invalid ReplicatedLogEntry count " + uaCount);
            }
            if (uaCount == 0) {
                return new ServerState(clusterConfig, ImmutableList.of());
            }

            final var uaBuilder = ImmutableList.<ReplicatedLogEntry>builderWithExpectedSize(uaCount);
            try (var ois = new ObjectInputStream(entryCompress.decodeInput(dis))) {
                long index = lastIncluded.index();
                for (int i = 0; i < uaCount; i++) {
                    final var term = ois.readLong();
                    final Payload payload;
                    try {
                        payload = (Payload) ois.readObject();
                    } catch (ClassNotFoundException e) {
                        throw new IOException("Cannot deserialize payload in entry " + i, e);
                    }
                    if (payload == null) {
                        throw new IOException("Unexpected null payload in entry " + i);
                    }
                    uaBuilder.add(new SimpleReplicatedLogEntry(++index, term, payload));
                }
            }
            return new ServerState(clusterConfig, uaBuilder.build());
        }
    }

    @Override
    public SnapshotSource dataSource() {
        return stateCompress.sourceFor(() -> {
            final var is = ByteStreams.limit(Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS), limit);
            is.skipNBytes(sso);
            return is;
        });
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("lastIncluded", lastIncluded)
            .add("timestamp", timestamp)
            .add("file", file)
            .add("size", limit)
            .add("entryCompress", entryCompress)
            .add("stateCompress", stateCompress)
            .toString();
    }
}
