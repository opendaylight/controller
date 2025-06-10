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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.function.LongFunction;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.JournaledLogEntry;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.raft.journal.EntryReader;
import org.opendaylight.raft.journal.FromByteBufMapper;
import org.opendaylight.raft.journal.RaftJournal;
import org.opendaylight.raft.journal.SegmentedRaftJournal;
import org.opendaylight.raft.journal.StorageLevel;
import org.opendaylight.raft.journal.ToByteBufMapper;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.TransientFile;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
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
public final class EntryJournalV1 implements AutoCloseable {
    /**
     * An entry in the metadata journal.
     */
    public record JournalMeta(long replayFrom, long applyTo) {
        // nothing else
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

    // TODO: this could be a lambda, but the interface does not give us the entry index and does not allow us to return
    //       anything, so we end up with this class
    private final class LogEntryWriter implements ToByteBufMapper<LogEntry> {
        private final long journalIndex;

        private WriteResult result = NoResult.INSTANCE;

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
                result = new FileResult(bodyFile);
            } else {
                result = InlineResult.INSTANCE;
            }
            return true;
        }
    }

    private static final class LogEntryReader extends AbstractRegistration implements EntryLoader {
        private final Path directory;
        private final EntryReader reader;

        LogEntryReader(final Path directory, final EntryReader reader) {
            this.directory = requireNonNull(directory);
            this.reader = requireNonNull(reader);
        }

        @Override
        public @Nullable JournaledLogEntry loadNext() {
            return reader.tryNext(this::bytesToEntry);
        }

        @Override
        protected void removeRegistration() {
            reader.close();
        }

        private JournaledLogEntry bytesToEntry(final long journalIndex, final ByteBuf buf) {
            try (var in = new ByteBufInputStream(buf)) {
                final var hdr = WritableObjects.readLongHeader(in);
                final var entryIndex = WritableObjects.readFirstLong(in, hdr);
                final var entryTerm = WritableObjects.readSecondLong(in, hdr);

                final var comp = hdr & COMPRESSION_MASK;
                final var compression = switch (comp) {
                    case COMPRESSION_NONE -> CompressionType.NONE;
                    case COMPRESSION_LZ4 -> CompressionType.LZ4;
                    default -> throw new IOException("Unrecognized compression " + comp);
                };

                final var disp = hdr & DISPOSITION_MASK;
                final var command = switch (disp) {
                    case DISPOSITION_FILE -> readFile(journalIndex, compression);
                    case DISPOSITION_INLINE -> readCommand(in, compression);
                    default -> throw new IOException("Unrecognized disposition " + disp);
                };

                return new JournaledLogEntry(entryIndex, entryTerm, command, journalIndex);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private Payload readFile(final long journalIndex, final CompressionType compression) throws IOException {
            try (var in = Files.newInputStream(directory.resolve(EntryJournalV1.ENTRY_FILE_MAME.apply(journalIndex)))) {
                return readCommand(in, compression);
            }
        }

        private static Payload readCommand(final InputStream in, final CompressionType compression) throws IOException {
            try (var ois = new ObjectInputStream(compression.decodeInput(in))) {
                try {
                    return (Payload) ois.readObject();
                } catch (ClassNotFoundException e) {
                    throw new IOException("Cannot read command", e);
                }
            }
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
            LOG.trace("{}: failed to write metadata entry", e);
            return false;
        }
        return true;
    };

    private final String memberId;
    private final Path directory;
    private final CompressionType compression;
    private final SegmentedRaftJournal metaJournal;
    private final SegmentedRaftJournal entryJournal;

    private long replayFrom = -1;
    private long applyTo = -1;

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

        try (var metaReader = metaJournal.openReader(-1)) {
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
        // FIXME: shutdown flusher first
        metaJournal.close();
    }

    public EntryLoader readEntries() {
        return new LogEntryReader(directory, entryJournal.openReader(replayFrom));
    }

    /**
     * Persists an entry to the applicable journal synchronously. The contract is that the callback will be invoked
     * before {@link RaftActor} sees any other message.
     *
     * @param entry the journal entry to persist
     * @param callback the callback when persistence is complete
     */
    public long persistEntry(final LogEntry entry) throws IOException {
        final var writer = entryJournal.writer();
        final var journalIndex = writer.nextIndex();
        final var mapper = new LogEntryWriter(journalIndex);
        writer.append(mapper, entry);

        switch (mapper.result) {
            // FIXME: defer flush
            case InlineResult inline -> writer.flush();
            case FileResult(var file) -> {
                // FIXME: move file to place and flush
                // FIXME: defer flush
                writer.flush();
                throw new UnsupportedOperationException();
            }
            // Internal error, modeled for non-nullness
            case NoResult no -> throw new IOException("Failed to write entry");
        }
        return journalIndex;
    }

    public void resetTo(long nextIndex) throws IOException {
        entryJournal.writer().reset(nextIndex);
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