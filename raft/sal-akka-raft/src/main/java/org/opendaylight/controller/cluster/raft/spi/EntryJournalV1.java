/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.function.LongFunction;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.raft.journal.FromByteBufMapper;
import org.opendaylight.raft.journal.RaftJournal;
import org.opendaylight.raft.journal.SegmentedRaftJournal;
import org.opendaylight.raft.journal.StorageLevel;
import org.opendaylight.raft.journal.ToByteBufMapper;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.TransientFile;
import org.opendaylight.yangtools.concepts.WritableObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Baseline implementation of a RAFT log journal. The storage is a bit complicated, as we need to balance imperfect
 * tools. The idea here is that we first recover a state snapshot and then replay any and all entries from the journal,
 * up to the last observed {@code commitIndex}.
 *
 * <p>The second part is not something RAFT mandates, but rather is an artifact of our current implementation
 * originating from the initial design: stable storage was organized around Pekko persistence with all its semantics.
 * This meant that we had no direct access to specific entry for the purposes of applying its state nor sending it to
 * peers and all entries were replayed during actor recovery, i.e. before we start participating in RAFT protocol and
 * those are kept on-heap.
 *
 * <p>In order to deal with all that we maintain a {@link RaftJournal} of metadata, tracking two indices:
 * <ol>
 *   <li>the first {@code journalIndex} to replay on recovery, and</li>
 *   <li>the {@code journalIndex} of the entry having the same index as the last observed RAFT {@code commitIndex}</li>
 * </ol>
 * We then use them to completely apply entries, effectively replaying our last actor state, which in turn allows us
 * to keep the on-heap state minimal.
 *
 * <p>The other part we maintain is a {@link RaftJournal} of individual entries, but there is a twist there as well:
 * since each journal requires a fixed maximum entry size, we specify that as {@code 256KiB} and provide our own dance
 * to store larger entries in separate files.
 */
@NonNullByDefault
public final class EntryJournalV1 {
    /**
     * An entry in the metadata journal.
     */
    private sealed interface MetaEntry {
        // nothing else
    }

    /**
     * Update the the 'replay from' journal index.
     */
    private record MetaReplayFrom(long journalIndex) implements MetaEntry {
        // Nothing else
    }

    /**
     * Update the the 'apply to' journal index.
     */
    private record MetaApplyTo(long journalIndex) implements MetaEntry {
        // Nothing else
    }

    // TODO: this could be a lambda, but the interface does not give us the entry index and does not allow us to return
    //       anything, so we end up with this class
    private final class LogEntryWriter implements ToByteBufMapper<LogEntry> {
        private final long journalIndex;

        @Nullable TransientFile file;

        LogEntryWriter(final long journalIndex) {
            this.journalIndex = journalIndex;
        }

        @Override
        public boolean objectToBytes(final LogEntry obj, final ByteBuf buf) throws IOException {
            // Require the buffer to be able to hold an entire inline entry. This leaves some unused space at the end of
            // the segment file, but makes other things a lot easier.
            final var writable = buf.writableBytes();
            if (writable < JOURNAL_INLINE_ENTRY_SIZE) {
                LOG.trace("Insufficient buffer size: {} available, {} required", writable, JOURNAL_INLINE_ENTRY_SIZE);
                return false;
            }

            // Write the header out first, as we want to it to be always present inline
            final var headerIndex = buf.writerIndex();
            final int headerSize;
            try (var bbos = new ByteBufOutputStream(buf)) {
                bbos.writeByte(switch (compression) {
                    case LZ4 -> HDR_IC;
                    case NONE -> HDR_IU;
                });
                WritableObjects.writeLongs(bbos, obj.index(), obj.term());
                headerSize = bbos.writtenBytes();
            }

            // Write the body out
            final TransientFile bodyFile;
            try (var out = new BufThenFileOutputStream(directory, journalIndex, ENTRY_FILE_MAME, buf,
                    JOURNAL_INLINE_ENTRY_SIZE - headerSize)) {
                try (var oos = new ObjectOutputStream(compression.encodeOutput(out))) {
                    oos.writeObject(obj.command().toSerialForm());
                }
                bodyFile = out.file();
            }

            if (bodyFile != null) {
                buf.setByte(headerIndex, switch (compression) {
                    case LZ4 -> HDR_FC;
                    case NONE -> HDR_FU;
                });
                file = bodyFile;
            }
            return true;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(EntryJournalV1.class);
    private static final HexFormat HF = HexFormat.of().withUpperCase();

    // 256KiB, which leads to reasonable heap buffers
    private static final int JOURNAL_INLINE_ENTRY_SIZE = 256 * 1024;
    // 128MiB, which leads to reasonable file sizes
    private static final int JOURNAL_SEGMENT_SIZE = JOURNAL_INLINE_ENTRY_SIZE * 512;

    static final LongFunction<String> ENTRY_FILE_MAME = journalIndex -> "entry-" + HF.toHexDigits(journalIndex) + ".v1";

    // Journal entry layout is quite simple:
    // - a leading byte, which indicates entry disposition and compression
    // - 1-17 bytes encoding the index and term on the entry, via
    //  {@link WritableObjects#writeLongs(DataOutput, long, long)}.
    // - serialized body (in case of DISPOSITION_INLINE)

    /**
     * The entry's command is serialized inline, following the index/term data.
     */
    static final byte DISPOSITION_INLINE = (byte) 0x00;
    /**
     * The entry's command is serialized in a separate file.
     */
    static final byte DISPOSITION_FILE   = (byte) 0x80;
    static final byte DISPOSITION_MASK   = DISPOSITION_FILE;

    /**
     * The entry's command is serialized without compression.
     */
    static final byte COMPRESSION_NONE   = (byte) 0x00;
    /**
     * The entry's command is serialized with LZ4 compression.
     */
    static final byte COMPRESSION_LZ4    = (byte) 0x40;
    static final byte COMPRESSION_MASK   = COMPRESSION_LZ4;

    // TODO: We have 0x3F, i.e. 6 bits, left in journal entry encoding. We should make use of them to:
    //       - guide stateful encoding:
    //         - one bit indicates contiguous index (= prevEntry.index + 1), acting as control to
    //           WritableObjects.readLong()
    //         - one bit indicates same term (= prevEntry.term), acting as control to WritableObjects.readLong()
    //       - carry (at least part of) the command type: in our default application we need about 10 possible values

    // Pre-computed header values for writeout
    private static final byte HDR_IU = (byte) (DISPOSITION_INLINE | COMPRESSION_NONE);
    private static final byte HDR_IC = (byte) (DISPOSITION_INLINE | COMPRESSION_LZ4);
    private static final byte HDR_FU = (byte) (DISPOSITION_FILE   | COMPRESSION_NONE);
    private static final byte HDR_FC = (byte) (DISPOSITION_FILE   | COMPRESSION_LZ4);

    // Metadata entries come in two flavors, indicated by the top-most bit in the first byte:
    // - set the first journal index to replay (i.e. all previous entries are in the snapshot)
    // - set the last journal index to apply (i.e. there is no consensus on latter entries)
    // The next 3 bits are reserved. The bottom-most 4 bits are the header produced by
    // {@link WritableObjects#writeLong(DataOutput, long, int)}, followed by 0-8 bytes containing the actual index.
    private static final byte META_REPLAY_FROM = (byte) 0x00;
    private static final byte META_APPLY_TO    = (byte) 0x80;
    private static final byte META_MASK        = META_APPLY_TO;

    private static final FromByteBufMapper<MetaEntry> META_ENTRY_READER = (index, bytes) -> {
        try (var in = new ByteBufInputStream(bytes)) {
            final var hdr = WritableObjects.readLongHeader(in);
            final var idx = WritableObjects.readLongBody(in, hdr);
            final var type = hdr & META_MASK;
            return switch (type) {
                case META_REPLAY_FROM -> new MetaReplayFrom(idx);
                case META_APPLY_TO -> new MetaApplyTo(idx);
                default -> throw new IOException("Unhandled type " + type);
            };
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    };

    private final String memberId;
    private final Path directory;
    private final CompressionType compression;
    private final SegmentedRaftJournal metaJournal;

    private long replayFrom = -1;
    private long applyTo = -1;

    public EntryJournalV1(final String memberId, final Path directory, final CompressionType compression,
            final boolean mapped) throws IOException {
        this.memberId = requireNonNull(memberId);
        this.directory = requireNonNull(directory);
        this.compression = requireNonNull(compression);

        // Open metadata and replay it to get our bounds
        metaJournal = openMetadata();
    }

    private SegmentedRaftJournal openMetadata() throws IOException {
        final var journal = SegmentedRaftJournal.builder()
            .withDirectory(directory)
            .withName("metadata-v1")
            // really only WritableObjects.writeLong(), e.g. 1-9 bytes
            .withMaxEntrySize(16)
            .withMaxSegmentSize(64 * 1024)
            .withStorageLevel(StorageLevel.DISK)
            .build();

        try (var metaReader = metaJournal.openReader(-1)) {
            while (true) {
                switch (metaReader.tryNext(META_ENTRY_READER)) {
                    case null -> {
                        LOG.debug("{}: metadata journal open: replayFrom={} applyTo={}", memberId, replayFrom, applyTo);
                        return journal;
                    }
                    case MetaReplayFrom(var index) -> replayFrom = index;
                    case MetaApplyTo(var index) -> applyTo = index;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            journal.close();
            throw new IOException("Failed to read metadata journal", e);
        }
    }
}
