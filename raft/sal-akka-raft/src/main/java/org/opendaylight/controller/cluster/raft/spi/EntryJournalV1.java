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

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.raft.journal.FromByteBufMapper;
import org.opendaylight.raft.journal.RaftJournal;
import org.opendaylight.raft.journal.SegmentedRaftJournal;
import org.opendaylight.raft.journal.StorageLevel;
import org.opendaylight.raft.journal.ToByteBufMapper;
import org.opendaylight.raft.spi.BufThenFileOutputStream;
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
 *
 * <p>Note: this introduces the concept of {@code journalIndex}, which currently defined to start with at {@code 1}.
 * Value {@code 0} is explicitly reserved for {@code unsigned long} overflow. Negative values yield undefined results.
 */
@NonNullByDefault
public final class EntryJournalV1 implements EntryJournal, AutoCloseable {
    /**
     * An entry in the metadata journal.
     */
    private record JournalMeta(long replayFrom, long applyTo) {
        JournalMeta {
            if (replayFrom < FIRST_JOURNAL_INDEX) {
                throw new IllegalArgumentException("replayFrom needs to be positive, not " + replayFrom);
            }
            if (applyTo < 0) {
                throw new IllegalArgumentException("applyTo needs to be non-negative, not " + replayFrom);
            }
        }
    }

    private sealed interface WriteResult {
        // Nothing else
    }

    private static final class NoResult implements WriteResult {
        static final WriteResult INSTANCE = new NoResult();
    }

    private sealed interface DoneResult extends WriteResult {

        long bodySize();
    }

    private record InlineResult(long bodySize) implements DoneResult {
        InlineResult {
            if (bodySize < 0) {
                throw new IllegalArgumentException("Bad body size" + bodySize);
            }
        }
    }

    private record FileResult(TransientFile file, long bodySize) implements DoneResult {
        FileResult {
            requireNonNull(file);
            if (bodySize < 0) {
                throw new IllegalArgumentException("Bad body size" + bodySize);
            }
        }
    }

    /**
     * A {@link JournalEntry} stored in a standalone file.
     */
    private static final class FileJournalEntry extends JournalEntry {
        private final Path file;

        FileJournalEntry(long index, long term, CompressionType compression, final Path file) throws IOException {
            super(index, term, compression);
            this.file = requireNonNull(file);
        }

        @Override
        protected InputStream newCommandStream() throws IOException {
            return new BufferedInputStream(Files.newInputStream(file));
        }

        @Override
        protected ToStringHelper addToStringAttributes(ToStringHelper helper) {
            return helper.add("file", file);
        }
    }

    /**
     * A {@link JournalEntry} stored directly in {@link RaftJournal}.
     */
    private static final class InlineJournalEntry extends JournalEntry {
        private final ByteBuf buf;

        InlineJournalEntry(long index, long term, CompressionType compression, ByteBuf buf) {
            super(index, term, compression);
            this.buf = requireNonNull(buf);
        }

        @Override
        protected InputStream newCommandStream() {
            return new ByteBufInputStream(buf.slice());
        }

        @Override
        protected ToStringHelper addToStringAttributes(final ToStringHelper helper) {
            return helper.add("size", buf.readableBytes());
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
                    case DISPOSITION_FILE -> {
                        final var file = directory.resolve(ENTRY_FILE_NAME.apply(index));
                        if (!Files.isReadable(file)) {
                            throw new IOException("File for entry at " + index + " is not readable");
                        }
                        yield new FileJournalEntry(entryIndex, entryTerm, comp, file);
                    }
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
            final long bodySize;
            try (var out = new BufThenFileOutputStream(directory, buf, JOURNAL_INLINE_ENTRY_SIZE - headerSize)) {
                try (var oos = new ObjectOutputStream(compression.encodeOutput(out))) {
                    oos.writeObject(obj.command().toSerialForm());
                }

                bodyFile = out.file();
                bodySize = out.size();
            }

            if (bodyFile != null) {
                buf.setByte(headerIndex, switch (compression) {
                    case LZ4 -> HDR_FC;
                    case NONE -> HDR_FU;
                });
                result = new FileResult(bodyFile, bodySize);
            } else {
                result = new InlineResult(bodySize);
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

    private static final LongFunction<String> ENTRY_FILE_NAME =
        journalIndex -> "entry-v1-" + HF.toHexDigits(journalIndex);

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

    private final ArrayDeque<Long> fileEntries = new ArrayDeque<>();
    private final String memberId;
    private final Path directory;
    private final CompressionType compression;
    private final SegmentedRaftJournal metaJournal;
    private final SegmentedRaftJournal entryJournal;

    private long replayFrom = FIRST_JOURNAL_INDEX;
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

        LOG.debug("{}: journal open: firstIndex={} lastIndex={} replayFrom={} fileEntries={}", memberId,
            entryJournal.firstIndex(), entryJournal.lastIndex(), replayFrom, fileEntries.size());
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
        // Open the journal
        final var journal = SegmentedRaftJournal.builder()
            .withDirectory(directory)
            .withName("journal-v1")
            .withMaxEntrySize(JOURNAL_INLINE_ENTRY_SIZE)
            .withMaxSegmentSize(JOURNAL_SEGMENT_SIZE)
            .withStorageLevel(mapped ? StorageLevel.MAPPED : StorageLevel.DISK)
            .build();

        try (var reader = openReader(journal)) {
            while (true) {
                final var journalIndex = reader.nextJournalIndex();
                final var journalEntry = reader.nextEntry();
                if (journalEntry == null) {
                    break;
                }
                if (journalEntry instanceof FileJournalEntry fileEntry) {
                    LOG.debug("{}: found entry file {}", memberId, fileEntry.file);
                    fileEntries.add(journalIndex);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            journal.close();
            throw new IOException("Failed to read entry journal", e);
        } catch (IOException e) {
            journal.close();
            throw e;
        }

        return journal;
    }

    public String memberId() {
        return memberId;
    }

    @Override
    public long applyToJournalIndex() {
        return applyTo;
    }

    @Override
    public void setApplyTo(final long newApplyTo) throws IOException {
        if (newApplyTo < 0) {
            throw new IOException("Bad applyTo index " + newApplyTo);
        }
        applyTo = newApplyTo;
        persistMeta();
    }

    @Override
    public Reader openReader() {
        return openReader(entryJournal);
    }

    // Split out for reuse during initialization
    private Reader openReader(final RaftJournal journal) {
        final var mapper = new LogEntryReader(directory);
        final var reader = journal.openReader(replayFrom);

        return new Reader() {
            @Override
            public long nextJournalIndex() {
                return reader.nextIndex();
            }

            @Override
            public @Nullable JournalEntry nextEntry() throws IOException {
                return reader.tryNext(mapper);
            }

            @Override
            public void close() {
                reader.close();
            }

            @Override
            public void resetToRead(long nextJournalIndex) throws IOException {
                if (nextJournalIndex < 1) {
                    throw new IOException("newJournalIndex needs to be positive, not " + nextJournalIndex);
                }
                reader.reset(nextJournalIndex);
            }
        };
    }

    public long nextToWrite() {
        return entryJournal.writer().nextIndex();
    }

    @Override
    public long appendEntry(final LogEntry entry) throws IOException {
        final var writer = entryJournal.writer();
        final var journalIndex = writer.nextIndex();
        final var mapper = new LogEntryWriter();
        writer.append(mapper, entry);

        final var ret = switch (mapper.result) {
            case InlineResult(var bodySize) -> bodySize;
            case FileResult(var file, var bodySize) -> {
                try {
                    Files.move(file.path(), directory.resolve(ENTRY_FILE_NAME.apply(journalIndex)),
                        ATOMIC_MOVE, REPLACE_EXISTING);
                } finally {
                    file.delete();
                }
                fileEntries.add(journalIndex);
                yield bodySize;
            }
            // Internal error, modeled for non-nullness
            case NoResult no -> throw new IOException("Failed to write entry");
        };

        // FIXME: defer flush
        writer.flush();
        return ret;
    }

    @Override
    public void discardHead(final long firstRetainedIndex) throws IOException {
        if (firstRetainedIndex < FIRST_JOURNAL_INDEX) {
            throw new IOException("Bad journal index " + firstRetainedIndex);
        }
        if (firstRetainedIndex <= replayFrom) {
            LOG.debug("{}: first entry {} already includes {}", memberId, replayFrom, firstRetainedIndex);
            return;
        }

        replayFrom = firstRetainedIndex;
        persistMeta();

        final var commitIndex = firstRetainedIndex - 1;
        entryJournal.writer().commit(commitIndex);
        entryJournal.compact(commitIndex);
        removeFiles(fileEntries.iterator(), idx -> idx > commitIndex);
        LOG.debug("{}: discarded entries up to (and including) {}", memberId(), commitIndex);
    }

    @Override
    public void discardTail(long firstRemovedIndex) throws IOException {
        if (firstRemovedIndex < FIRST_JOURNAL_INDEX) {
            throw new IOException("Bad journal index " + firstRemovedIndex);
        }
        if (firstRemovedIndex < replayFrom) {
            throw new IOException("First available entry is " + replayFrom + ", cannot discard tail starting at "
                + firstRemovedIndex);
        }

        // Trim the journal
        entryJournal.writer().reset(firstRemovedIndex);

        // Trim applyTo if needed
        if (applyTo >= firstRemovedIndex) {
            applyTo = firstRemovedIndex - 1;
            persistMeta();
        }

        // Remove any files
        removeFiles(fileEntries.descendingIterator(), idx -> idx < firstRemovedIndex);
    }

    @Override
    public void close() {
        entryJournal.close();
        metaJournal.close();
        LOG.debug("{}: journal closed", memberId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("memberId", memberId).toString();
    }

    private void persistMeta() throws IOException {
        final var meta = new JournalMeta(replayFrom, applyTo);
        final var writer = metaJournal.writer();
        final var metaIndex = writer.nextIndex();
        writer.append(META_ENTRY_WRITER, meta);
        writer.commit(metaIndex);
        metaJournal.compact(metaIndex);
        LOG.debug("{}: updated meta to {}", memberId, meta);
    }

    private void removeFiles(final Iterator<Long> it, final LongPredicate stopPredicate) throws IOException {
        while (it.hasNext()) {
            final long index = it.next();
            if (stopPredicate.test(index)) {
                break;
            }

            final var file = directory.resolve(ENTRY_FILE_NAME.apply(index));
            Files.deleteIfExists(file);
            LOG.debug("{}: removed file {}", memberId, file);
            it.remove();
        }
    }
}
