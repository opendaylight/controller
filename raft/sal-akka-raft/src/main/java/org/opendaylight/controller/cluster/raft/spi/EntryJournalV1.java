/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.function.LongFunction;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.ReplicatedLog.Recovery;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.journal.FromByteBufMapper;
import org.opendaylight.raft.journal.RaftJournal;
import org.opendaylight.raft.journal.SegmentedRaftJournal;
import org.opendaylight.raft.journal.StorageLevel;
import org.opendaylight.raft.journal.ToByteBufMapper;
import org.opendaylight.raft.spi.BufThenFileOutputStream;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.TransientFile;
import org.opendaylight.yangtools.concepts.Immutable;
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
 *
 * <p>Note: this introduces the concept of {@code journalIndex}, which currently defined to start with at {@code 1}.
 * Value {@code 0} is explicitly reserved for {@code unsigned long} overflow. Negative values yield undefined results.
 */
@NonNullByDefault
public final class EntryJournalV1 implements AutoCloseable {
    /**
     * An entry in the metadata journal.
     */
    public record JournalMeta(long replayFrom, long applyTo) {
        public JournalMeta {
            if (replayFrom < 1) {
                throw new IllegalArgumentException("replayFrom needs to be positive, not " + replayFrom);
            }
            if (applyTo < 0) {
                throw new IllegalArgumentException("replayFrom needs to be non-negative, not " + replayFrom);
            }
        }
    }

    private sealed interface WriteResult {
        // Nothing else
    }

    private static final class NoResult implements WriteResult {
        static final WriteResult INSTANCE = new NoResult();
    }

    private static final class InlineResult implements WriteResult {
        static final WriteResult INSTANCE = new InlineResult();
    }

    private record FileResult(TransientFile file) implements WriteResult {
        FileResult {
            requireNonNull(file);
        }
    }

    private abstract static sealed class JournalEntry implements Immutable {
        private final long index;
        private final long term;
        private final CompressionType compression;

        private JournalEntry(long index, long term, CompressionType compression) {
            this.index = index;
            this.term = term;
            this.compression = requireNonNull(compression);
        }

        final LogEntry toLogEntry() throws IOException {
            final StateMachineCommand command;
            try (var ois = new ObjectInputStream(compression.decodeInput(openBufferedStream()))) {
                try {
                    command = requireNonNull((StateMachineCommand) ois.readObject());
                } catch (ClassNotFoundException e) {
                    throw new IOException("Cannot read command", e);
                }
            }
            return new DefaultLogEntry(index, term, command);
        }

        abstract InputStream openBufferedStream() throws IOException;
    }

    private static final class FileJournalEntry extends JournalEntry {
        private final Path file;

        FileJournalEntry(long index, long term, CompressionType compression, final Path file) throws IOException {
            super(index, term, compression);
            this.file = requireNonNull(file);
        }

        Path file() {
            return file;
        }

        @Override
        InputStream openBufferedStream() throws IOException {
            return new BufferedInputStream(Files.newInputStream(file));
        }
    }

    private static final class InlineJournalEntry extends JournalEntry {
        private final ByteBuf buf;

        InlineJournalEntry(long index, long term, CompressionType compression, ByteBuf buf) {
            super(index, term, compression);
            this.buf = requireNonNull(buf);
        }

        @Override
        InputStream openBufferedStream() {
            return new ByteBufInputStream(buf.slice());
        }
    }

    private static final class LogEntryReader implements FromByteBufMapper<JournalEntry> {
        private final Path directory;

        LogEntryReader(final Path directory) {
            this.directory = requireNonNull(directory);
        }

        @Override
        public JournalEntry bytesToObject(final long index, final ByteBuf bytes) {
            try {
                final CompressionType comp;
                final long entryIndex;
                final long entryTerm;
                final int dispBits;

                try (var in = new ByteBufInputStream(bytes)) {
                    final var flags = in.readByte();
                    final var compBits = flags & COMPRESSION_MASK;
                    comp = switch (compBits) {
                        case COMPRESSION_NONE -> CompressionType.NONE;
                        case COMPRESSION_LZ4 -> CompressionType.LZ4;
                        default -> throw new IOException("Unrecognized compression " + compBits);
                    };
                    dispBits = flags & DISPOSITION_MASK;
                    final var hdr = WritableObjects.readLongHeader(in);
                    entryIndex = WritableObjects.readFirstLong(in, hdr);
                    entryTerm = WritableObjects.readSecondLong(in, hdr);
                }

                return switch (dispBits) {
                    case DISPOSITION_FILE ->
                        new FileJournalEntry(entryIndex, entryTerm, comp,
                            directory.resolve(ENTRY_FILE_MAME.apply(index)));
                    case DISPOSITION_INLINE -> new InlineJournalEntry(entryIndex, entryTerm, comp, bytes.slice());
                    default -> throw new IOException("Unrecognized disposition " + dispBits);
                };
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    // TODO: this could be a lambda, but the interface does not give us the entry index and does not allow us to return
    //       anything, so we end up with this class
    private final class LogEntryWriter implements ToByteBufMapper<LogEntry> {
        // Pre-computed header values for writeout
        private static final byte HDR_IU = (byte) (DISPOSITION_INLINE | COMPRESSION_NONE);
        private static final byte HDR_IC = (byte) (DISPOSITION_INLINE | COMPRESSION_LZ4);
        private static final byte HDR_FU = (byte) (DISPOSITION_FILE   | COMPRESSION_NONE);
        private static final byte HDR_FC = (byte) (DISPOSITION_FILE   | COMPRESSION_LZ4);

        private WriteResult result = NoResult.INSTANCE;

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
            try (var out = new BufThenFileOutputStream(directory, buf, JOURNAL_INLINE_ENTRY_SIZE - headerSize)) {
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
                result = new FileResult(bodyFile);
            } else {
                result = InlineResult.INSTANCE;
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
    private static final byte DISPOSITION_INLINE = (byte) 0x00;
    /**
     * The entry's command is serialized in a separate file.
     */
    private static final byte DISPOSITION_FILE   = (byte) 0x80;
    private static final byte DISPOSITION_MASK   = DISPOSITION_FILE;

    /**
     * The entry's command is serialized without compression.
     */
    private static final byte COMPRESSION_NONE   = (byte) 0x00;
    /**
     * The entry's command is serialized with LZ4 compression.
     */
    private static final byte COMPRESSION_LZ4    = (byte) 0x40;
    private static final byte COMPRESSION_MASK   = COMPRESSION_LZ4;

    // TODO: We have 0x3F, i.e. 6 bits, left in journal entry encoding. We should make use of them to:
    //       - guide stateful encoding:
    //         - one bit indicates contiguous index (= prevEntry.index + 1), acting as control to
    //           WritableObjects.readLong()
    //         - one bit indicates same term (= prevEntry.term), acting as control to WritableObjects.readLong()
    //       - carry (at least part of) the command type: in our default application we need about 10 possible values

    private static final FromByteBufMapper<JournalMeta> META_ENTRY_READER = (index, bytes) -> {
        try (var in = new ByteBufInputStream(bytes)) {
            final var hdr = WritableObjects.readLongHeader(in);
            return new JournalMeta(WritableObjects.readFirstLong(in, hdr), WritableObjects.readSecondLong(in, hdr));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read metadata entry", e);
        }
    };

    private static final ToByteBufMapper<JournalMeta> META_ENTRY_WRITER = (obj, buf) -> {
        try (var out = new ByteBufOutputStream(buf)) {
            WritableObjects.writeLongs(out, obj.replayFrom, obj.applyTo);
        } catch (IndexOutOfBoundsException e) {
            LOG.trace("Failed to write metadata entry", e);
            return false;
        }
        return true;
    };

    private final String memberId;
    private final Path directory;
    private final CompressionType compression;
    private final SegmentedRaftJournal metaJournal;
    private final SegmentedRaftJournal entryJournal;

    private long replayFrom = 1;
    private long applyTo = 0;

    public EntryJournalV1(final String memberId, final Path directory, final CompressionType compression,
            final boolean mapped) throws IOException {
        this.memberId = requireNonNull(memberId);
        this.directory = requireNonNull(directory);
        this.compression = requireNonNull(compression);

        // Open metadata and replay it to get our bounds
        metaJournal = openMetadata();

        try {
            entryJournal = openEntries(mapped);
        } catch (IOException e) {
            metaJournal.close();
            throw e;
        }

        LOG.info("{}: journal open: firstIndex={} lastIndex={} replayFrom={}", memberId, entryJournal.firstIndex(),
            entryJournal.lastIndex(), replayFrom);
    }

    private SegmentedRaftJournal openMetadata() throws IOException {
        final var journal = SegmentedRaftJournal.builder()
            .withDirectory(directory)
            .withName("metadata-v1")
            // really only WritableObjects.writeLongs(), e.g. 1-17 bytes
            .withMaxEntrySize(17)
            .withMaxSegmentSize(64 * 1024)
            .withStorageLevel(StorageLevel.DISK)
            .build();

        try (var metaReader = journal.openReader(-1)) {
            while (true) {
                final var entry = metaReader.tryNext(META_ENTRY_READER);
                if (entry == null) {
                    LOG.debug("{}: metadata journal open: replayFrom={} applyTo={}", memberId, replayFrom, applyTo);
                    return journal;
                }
                replayFrom = entry.replayFrom;
                applyTo = entry.applyTo;
            }
        } catch (IndexOutOfBoundsException e) {
            journal.close();
            throw new IOException("Failed to read metadata journal", e);
        }
    }

    private SegmentedRaftJournal openEntries(final boolean mapped) throws IOException {
        return SegmentedRaftJournal.builder()
            .withDirectory(directory)
            .withName("journal-v1")
            .withMaxEntrySize(JOURNAL_INLINE_ENTRY_SIZE)
            .withMaxSegmentSize(JOURNAL_SEGMENT_SIZE)
            .withStorageLevel(mapped ? StorageLevel.MAPPED : StorageLevel.DISK)
            .build();
    }

    public String memberId() {
        return memberId;
    }

    public long applyTo() {
        return applyTo;
    }

    @Override
    public void close() {
        entryJournal.close();
        metaJournal.close();
    }

    public void recoverTo(final Recovery recovery, final EntryInfo snapshotEntry) throws IOException {
        final var mapper = new LogEntryReader(directory);
        final var reader = entryJournal.openReader(replayFrom);

        recovery.initiallizedPosition(snapshotEntry, replayFrom, applyTo);
        var expectedIndex = snapshotEntry.index() + 1;

        while (true) {
            final var journalIndex = reader.nextIndex();
            final var journalEntry = reader.tryNext(mapper);
            if (journalEntry == null) {
                recovery.finishRecovery();
                return;
            }
            final var logEntry = journalEntry.toLogEntry();
            final var entryIndex = logEntry.index();
            if (entryIndex != expectedIndex) {
                throw new IOException("Mismatched entry index=%s expecting %s at journalIndex=%s".formatted(
                    entryIndex, expectedIndex, journalIndex));
            }

            recovery.recoverEntry(logEntry.term(), logEntry.command());
            expectedIndex++;
        }
    }

    /**
     * Persists an entry to the applicable journal synchronously. The contract is that the callback will be invoked
     * before {@link RaftActor} sees any other message.
     *
     * @param entry the journal entry to persist
     */
    public long persistEntry(final LogEntry entry) throws IOException {
        final var writer = entryJournal.writer();
        final var journalIndex = writer.nextIndex();
        final var mapper = new LogEntryWriter();
        writer.append(mapper, entry);

        switch (mapper.result) {
            // FIXME: defer flush
            case InlineResult inline -> {
                // No-op
            }
            case FileResult(var file) -> {
                try {
                    Files.move(file.path(), directory.resolve(ENTRY_FILE_MAME.apply(journalIndex)),
                        ATOMIC_MOVE, REPLACE_EXISTING);
                } finally {
                    file.delete();
                }
            }
            // Internal error, modeled for non-nullness
            case NoResult no -> throw new IOException("Failed to write entry");
        }

        // FIXME: defer flush
        writer.flush();
        return journalIndex;
    }

    public void resetTo(long nextIndex) throws IOException {
        entryJournal.writer().reset(nextIndex);
        if (applyTo >= nextIndex) {
            applyTo = nextIndex - 1;
        }
    }

    public void setApplyTo(final long lastApplied) throws IOException {
        applyTo = lastApplied;
        persistMeta();
    }

    public void setReplayFrom(final long fromIndex) throws IOException {
        replayFrom = fromIndex;
        persistMeta();
        entryJournal.writer().commit(fromIndex);
        entryJournal.compact(fromIndex);
    }

    private void persistMeta() throws IOException {
        metaJournal.writer().append(META_ENTRY_WRITER, new JournalMeta(replayFrom, applyTo));
        // FIXME: trim if needed
    }
}