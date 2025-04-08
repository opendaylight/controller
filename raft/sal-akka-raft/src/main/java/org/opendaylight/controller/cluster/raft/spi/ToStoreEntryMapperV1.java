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
import com.google.common.base.MoreObjects;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.function.LongFunction;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.ToStoreEntry.ToFile;
import org.opendaylight.controller.cluster.raft.spi.ToStoreEntry.ToJournal;
import org.opendaylight.raft.journal.RaftJournal;
import org.opendaylight.raft.journal.ToByteBufMapper;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.TransientFile;
import org.opendaylight.yangtools.concepts.WritableObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ToByteBufMapper} mapping {@link ReplicatedLogEntry} to persistent storage. Our strategy here relies on the
 * fact we have a per-RAFT-server persistence directory. We allocate two file prefixes:
 * <ol>
 *   <li>{@code journal} for {@link RaftJournal} segments</li>
 *   <li>{@code entry-} for individual entries</li>
 * </ol>
 * The {@code threshold} should match RaftJournal maximum entry size and should match its directory.
 */
@NonNullByDefault
final class ToStoreEntryMapperV1 implements ToByteBufMapper<ReplicatedLogEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(ToStoreEntryMapperV1.class);
    private static final String ENTRY_PREFIX = "entry-";
    private static final HexFormat HF = HexFormat.of().withUpperCase();
    private static final LongFunction<String> ENTRY_FILE_MAME =
        journalIndex -> ENTRY_PREFIX + HF.toHexDigits(journalIndex) + ".v1";

    /**
     * Minimum threshold we support. We assert that we need to hold in journal:
     * <ol>
     *   <li>1 header byte</li>
     *   <li>{@code WritableObjects.writeLongs()}, i.e. 17 bytes</li>
     * </ol>
     */
    @VisibleForTesting
    static final int THRESHOLD_MIN = 1 + 17;

    // Header byte constants:
    // - top (sign) bit indicates location
    static final byte LOCATION_MASK    = (byte) 0x80;
    static final byte LOCATION_JOURNAL = (byte) 0x00;
    static final byte LOCATION_FILE    = LOCATION_MASK;
    // - second-to-top bit indicates compression
    static final byte COMPRESSION_MASK = 0x40;
    static final byte COMPRESSION_NONE = 0x00;
    static final byte COMPRESSION_LZ4  = 0x40;
    // - we have 6 reserved bits left, defined to be zero
    static final byte RESERVED_MASK    = 0x3F;

    // TODO: make use of the above bits to
    //       - guide stateful encoding:
    //         - one bit indicates contiguous index (= prevEntry.index + 1), acting as control to
    //           WritableObjects.readLong()
    //         - one bit indicates same term (= prevEntry.term), acting as control to WritableObjects.readLong()
    //       - carry (at least part of) the command type -- for what we need at least 10 possible values

    // Pre-computed constants for writeout
    private static final int LZ4_FILE     = LOCATION_FILE | COMPRESSION_LZ4;
    private static final int NONE_FILE    = LOCATION_FILE | COMPRESSION_NONE;
    private static final int LZ4_JOURNAL  = LOCATION_JOURNAL | COMPRESSION_LZ4;
    private static final int NONE_JOURNAL = LOCATION_JOURNAL | COMPRESSION_NONE;

    private final ArrayList<ToStoreEntry> storedEntries = new ArrayList<>();
    private final CompressionType compression;
    private final String logName;
    private final Path directory;
    private final int threshold;

    private long journalIndex;

    ToStoreEntryMapperV1(final String logName, final Path directory, final int threshold,
            final CompressionType compression, final long journalIndex) {
        this.logName = requireNonNull(logName);
        this.directory = requireNonNull(directory);
        this.compression = requireNonNull(compression);
        if (threshold < THRESHOLD_MIN) {
            throw new IllegalArgumentException("Invalid threshold " + threshold);
        }
        this.threshold = threshold;
        this.journalIndex = journalIndex;
    }

    @Override
    public void objectToBytes(final ReplicatedLogEntry obj, final ByteBuf buf) throws IOException {
        if (buf.writableBytes() >= threshold) {
            doObjectToBytes(obj, buf.slice(0, threshold));
        } else {
            tryObjectToBytes(obj, buf);
        }
        journalIndex++;
    }

    // we can write at least threshold bytes: we should not report EOFException
    private void doObjectToBytes(final ReplicatedLogEntry obj, final ByteBuf buf) throws IOException {
        LOG.trace("{}: safe entry writeout", logName);
        writeToJournal(obj, buf);
    }

    // we cannot write threshold bytes, so may issue an EOFException
    private void tryObjectToBytes(final ReplicatedLogEntry obj, final ByteBuf buf) throws IOException {
        LOG.trace("{}: careful entry writeout", logName);
        try {
            writeToJournal(obj, buf);
        } catch (IndexOutOfBoundsException e) {
            throw eof(e);
        }
    }

    private void writeToJournal(final ReplicatedLogEntry obj, final ByteBuf buf) throws IOException {
        final var headerIndex = buf.writerIndex();
        // Header
        buf.writeByte(switch (compression) {
            case LZ4 -> LZ4_JOURNAL;
            case NONE -> NONE_JOURNAL;
        });

        // EntryMeta
        try (var bbos = new ByteBufOutputStream(buf)) {
            WritableObjects.writeLongs(bbos, obj.index(), obj.term());
        }

        final var headerSize = buf.writerIndex() - headerIndex;
        final TransientFile bodyFile;
        final long bodySize;
        // Command
        try (var bos = new BufThenFileOutputStream(directory, journalIndex, ENTRY_FILE_MAME, buf,
                threshold - headerSize)) {
            try (var oos = new ObjectOutputStream(compression.encodeOutput(bos))) {
                oos.writeObject(obj.command().toSerialForm());
            }

            bodyFile = bos.file();
            bodySize = bos.size();
        }

        final long size = headerSize + bodySize;
        final ToStoreEntry entry;
        if (bodyFile != null) {
            LOG.trace("{}: entry written to {} in-journal bytes and {} in-file bytes", logName, headerSize, bodySize);
            entry = new ToFile(obj, size, bodyFile);

            // Overwrite the first byte to indicate file storage and reset writerIndex to contain only the header
            buf.setByte(headerIndex, switch (compression) {
                case LZ4 -> LZ4_FILE;
                case NONE -> NONE_FILE;
            });
        } else {
            LOG.trace("{}: entry written to {} in-journal bytes", logName, size);
            entry = new ToJournal(obj, size);
        }
        storedEntries.add(entry);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("directory", directory)
            .add("threshold", threshold)
            .add("storedEntries", storedEntries.size())
            .toString();
    }

    private static EOFException eof(final Throwable cause) {
        final var eof = new EOFException();
        eof.initCause(cause);
        return eof;
    }
}
