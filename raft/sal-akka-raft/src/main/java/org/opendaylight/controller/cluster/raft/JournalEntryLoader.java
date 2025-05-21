/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.controller.cluster.raft.PekkoRaftStorage.COMPRESSION_LZ4;
import static org.opendaylight.controller.cluster.raft.PekkoRaftStorage.COMPRESSION_MASK;
import static org.opendaylight.controller.cluster.raft.PekkoRaftStorage.COMPRESSION_NONE;
import static org.opendaylight.controller.cluster.raft.PekkoRaftStorage.DISPOSITION_FILE;
import static org.opendaylight.controller.cluster.raft.PekkoRaftStorage.DISPOSITION_INLINE;
import static org.opendaylight.controller.cluster.raft.PekkoRaftStorage.DISPOSITION_MASK;
import static org.opendaylight.controller.cluster.raft.PekkoRaftStorage.TYPE_LAST_APPLIED;
import static org.opendaylight.controller.cluster.raft.PekkoRaftStorage.TYPE_LOG_ENTRY;
import static org.opendaylight.controller.cluster.raft.PekkoRaftStorage.TYPE_MASK;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.spi.EntryLoader;
import org.opendaylight.raft.journal.EntryReader;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.WritableObjects;

@NonNullByDefault
final class JournalEntryLoader extends AbstractRegistration implements EntryLoader {
    private final Path directory;
    private final EntryReader reader;

    JournalEntryLoader(final Path directory, final EntryReader reader) {
        this.directory = requireNonNull(directory);
        this.reader = requireNonNull(reader);
    }

    @Override
    public @Nullable LoadedEntry loadNext() {
        return reader.tryNext(this::bytesToEntry);
    }

    @Override
    protected void removeRegistration() {
        reader.close();
    }

    private LoadedEntry bytesToEntry(final long journalIndex, final ByteBuf buf) {
        try (var in = new ByteBufInputStream(buf)) {
            final var header = in.readByte();
            final var type = header & TYPE_MASK;
            return switch (type) {
                case TYPE_LOG_ENTRY -> bytesToLogEntry(journalIndex, in, header);
                case TYPE_LAST_APPLIED -> new LoadedLastApplied(journalIndex, WritableObjects.readLongBody(in, header));
                default -> throw new IllegalArgumentException("Unhandled type " + type);
            };
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private LoadedLogEntry bytesToLogEntry(final long journalIndex, final ByteBufInputStream in, final byte header)
            throws IOException {
        final var hdr = WritableObjects.readLongHeader(in);
        final var entryIndex = WritableObjects.readFirstLong(in, hdr);
        final var entryTerm = WritableObjects.readSecondLong(in, hdr);

        final var comp = header & COMPRESSION_MASK;
        final var compression = switch (comp) {
            case COMPRESSION_NONE -> CompressionType.NONE;
            case COMPRESSION_LZ4 -> CompressionType.LZ4;
            default -> throw new IOException("Unrecognized compression " + comp);
        };

        final var disp = header & DISPOSITION_MASK;
        final var command = switch (disp) {
            case DISPOSITION_FILE -> readFile(journalIndex, compression);
            case DISPOSITION_INLINE -> readCommand(in, compression);
            default -> throw new IOException("Unrecognized disposition " + disp);
        };

        return new LoadedLogEntry(journalIndex, entryIndex, entryTerm, command);
    }

    private Payload readFile(final long journalIndex, final CompressionType compression) throws IOException {
        try (var in = Files.newInputStream(directory.resolve(directory))) {
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