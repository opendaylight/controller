/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EntryLoader;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.raft.journal.SegmentedRaftJournal;
import org.opendaylight.raft.journal.StorageLevel;
import org.opendaylight.raft.journal.ToByteBufMapper;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.opendaylight.raft.spi.TransientFile;
import org.opendaylight.yangtools.concepts.WritableObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link EnabledRaftStorage} backed by Pekko Persistence of an {@link RaftActor}.
 */
// FIXME: remove this class once we have both Snapshots and Entries stored in files
final class PekkoRaftStorage extends EnabledRaftStorage {

    sealed interface JournalAction {

    }

    private record JournalAppendEntry() implements JournalAction {

    }

    private record JournalDeleteEntries() implements JournalAction {

    }

    private record JournalMarkLastApplied() implements JournalAction {

    }

    private record JournalReset() implements JournalAction {

    }

    private final class LogEntryMapper implements ToByteBufMapper<@NonNull LogEntry> {
        private final long journalIndex;

        @Nullable TransientFile file;

        LogEntryMapper(final long journalIndex) {
            this.journalIndex = journalIndex;
        }

        @Override
        public boolean objectToBytes(final LogEntry obj, final ByteBuf buf) throws IOException {
            // Require the buffer to be able to hold an entire inline entry. This leaves some unused space at the end of
            // the segment file, but makes other things a lot easier.
            final var writable = buf.writableBytes();
            if (writable < INLINE_ENTRY_SIZE) {
                LOG.trace("Insufficient buffer size: {} available, {} required", writable, INLINE_ENTRY_SIZE);
                return false;
            }

            // Write the header out first, as we want to it to be always present inline
            final var headerIndex = buf.writerIndex();
            final int headerSize;
            try (var bbos = new ByteBufOutputStream(buf)) {
                bbos.writeByte(switch (compression) {
                    case LZ4 -> HDR_LE_IC;
                    case NONE -> HDR_LE_IU;
                });
                WritableObjects.writeLongs(bbos, obj.index(), obj.term());
                headerSize = bbos.writtenBytes();
            }

            // Write the body out
            final TransientFile bodyFile;
            try (var out = new BufThenFileOutputStream(directory, journalIndex, ENTRY_FILE_MAME, buf,
                    INLINE_ENTRY_SIZE - headerSize)) {
                try (var oos = new ObjectOutputStream(compression.encodeOutput(out))) {
                    oos.writeObject(obj.command().toSerialForm());
                }
                bodyFile = out.file();
            }

            if (bodyFile != null) {
                buf.setByte(headerIndex, switch (compression) {
                    case LZ4 -> HDR_LE_FC;
                    case NONE -> HDR_LE_FU;
                });
                file = bodyFile;
            }
            return true;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(PekkoRaftStorage.class);

    private static final String JOURNAL_NAME = "entries-v1";
    private static final String ENTRY_PREFIX = "entry-";
    private static final HexFormat HF = HexFormat.of().withUpperCase();
    private static final LongFunction<String> ENTRY_FILE_MAME =
        journalIndex -> ENTRY_PREFIX + HF.toHexDigits(journalIndex) + ".v1";

    // 256KiB, which leads to reasonable heap buffers
    private static final int INLINE_ENTRY_SIZE = 256 * 1024;
    // 128MiB, which leads to reasonable file sizes
    private static final int SEGMENT_SIZE = INLINE_ENTRY_SIZE * 512;

    // A few bits indicating type, disposition and compression of a particular stored entry. The idea here is that we
    // read the first inline byte of a stored entry and interpret it in one of two ways based on whether or not
    // the topmost is set:

    /**
     * A {@link LogEntry} to be appended to {@link ReplicatedLog}. The other 7 bits should be interpreted as disposition
     * and compression. The leading byte is followed by 1-17 bytes encoding the index and term on the entry, via
     * {@link WritableObjects#writeLongs(DataOutput, long, long)}.
     */
    static final byte TYPE_LOG_ENTRY     = (byte) 0x00;
    /**
     * A change in {@code lastApplied} index, to be propagated to {@link ReplicatedLog#setLastApplied(long)}. The next
     * top-most 3 bits are reserved: they should be set to 0 on writeout and ignored on read. The bottom-most 4 bits are
     * the header produced by {@link WritableObjects#writeLong(DataOutput, long, int)}, followed by 0-8 bytes containing
     * the actual index.
     */
    static final byte TYPE_LAST_APPLIED  = (byte) 0x80;
    static final byte TYPE_MASK          = TYPE_LAST_APPLIED;

    /**
     * The entry's command is serialized inline, following the index/term data.
     */
    static final byte DISPOSITION_INLINE = (byte) 0x00;
    /**
     * The entry's command is serialized in a separate file.
     */
    static final byte DISPOSITION_FILE   = (byte) 0x40;
    static final byte DISPOSITION_MASK   = DISPOSITION_FILE;

    /**
     * The entry's command is serialized without compression.
     */
    static final byte COMPRESSION_NONE   = (byte) 0x00;
    /**
     * The entry's command is serialized with LZ4 compression.
     */
    static final byte COMPRESSION_LZ4    = (byte) 0x20;
    static final byte COMPRESSION_MASK   = COMPRESSION_LZ4;

    // TODO: We have 0x1F, i.e. 5 bits, left in TYPE_LOG_ENTRY encoding. We should make use of them to:
    //       - guide stateful encoding:
    //         - one bit indicates contiguous index (= prevEntry.index + 1), acting as control to
    //           WritableObjects.readLong()
    //         - one bit indicates same term (= prevEntry.term), acting as control to WritableObjects.readLong()
    //       - carry (at least part of) the command type: in our default application we need about 10 possible values

    // Pre-computed header values for writeout
    private static final byte HDR_LE_IU = (byte) (TYPE_LOG_ENTRY | DISPOSITION_INLINE | COMPRESSION_NONE);
    private static final byte HDR_LE_IC = (byte) (TYPE_LOG_ENTRY | DISPOSITION_INLINE | COMPRESSION_LZ4);
    private static final byte HDR_LE_FU = (byte) (TYPE_LOG_ENTRY | DISPOSITION_FILE   | COMPRESSION_NONE);
    private static final byte HDR_LE_FC = (byte) (TYPE_LOG_ENTRY | DISPOSITION_FILE   | COMPRESSION_LZ4);

    private final boolean mapped;

    private SegmentedRaftJournal journal;

    @NonNullByDefault
    PekkoRaftStorage(final RaftActor actor, final Path directory, final CompressionType compression,
            final Configuration streamConfig, final boolean mapped) {
        super(actor.memberId(), actor, directory, compression, streamConfig);
        this.mapped = mapped;
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
            .withMaxSegmentSize(SEGMENT_SIZE)
            .withStorageLevel(mapped ? StorageLevel.MAPPED : StorageLevel.DISK)
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
    public EntryLoader openLoader() {
        return new JournalEntryLoader(directory, journal.openReader(-1));
    }

    @Override
    public long persistEntry(final ReplicatedLogEntry entry) throws IOException {
        final var writer = journal.writer();
        final var mapper = new LogEntryMapper(writer.nextIndex());
        writer.append(mapper, entry);

//        final var file = mapper.file;

        throw new UnsupportedOperationException();
    }

    @Override
    public void startPersistEntry(final ReplicatedLogEntry entry, final LongConsumer callback) {
        requireNonNull(callback);
        // FIXME: asynchronous
        final long journalIndex;
        try {
            journalIndex = persistEntry(entry);
        } catch (IOException e) {
            LOG.error("{}: to persist entry", memberId, e);
            throw new UncheckedIOException(e);
        }
        actor.executeInSelf(() -> callback.accept(journalIndex));
    }

    @Override
    public void deleteEntries(final long fromIndex) {
        journal.writer().reset(fromIndex);
    }

    @Override
    public void markLastApplied(final long lastApplied) {
        try {
            journal.writer().append(PekkoRaftStorage::writeLastApplied, lastApplied);
        } catch (IOException e) {
            LOG.error("{}: failed to mark last applied index", memberId, e);
            throw new UncheckedIOException(e);
        }
        LOG.debug("{}: update commit-index to {}", memberId, lastApplied);
    }

    @Override
    public void deleteMessages(final long sequenceNumber) {
        journal.writer().commit(sequenceNumber);
        journal.compact(sequenceNumber);
    }

    @Override
    public long lastSequenceNumber() {
        return journal.lastIndex();
    }

    private static boolean writeLastApplied(final Long lastApplied, final ByteBuf buf) throws IOException {
        try (var out = new ByteBufOutputStream(buf)) {
            WritableObjects.writeLong(out, lastApplied, TYPE_LAST_APPLIED & 0xF0);
        } catch (IndexOutOfBoundsException e) {
            LOG.trace("Not enough buffer space", e);
            return false;
        }
        return true;
    }
}
