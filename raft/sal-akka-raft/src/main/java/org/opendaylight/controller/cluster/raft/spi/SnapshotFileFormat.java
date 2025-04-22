/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.api.TermInfo;
import org.opendaylight.raft.spi.CompressionType;

/**
 * All supported snapshot file formats. Content closely mirrors {@link Snapshot}, but does not include {@link TermInfo}
 */
@NonNullByDefault
public enum SnapshotFileFormat {
    /**
     * First version of snapshot file format.
     */
    SNAPSHOT_V1(".v1") {
        @Override
        public <T extends StateSnapshot> void createNew(final Path file, final Instant timestamp,
                final EntryInfo lastIncluded, final @Nullable VotingConfig votingConfig,
                final CompressionType entryCompress, final List<ReplicatedLogEntry> unappliedEntries,
                final CompressionType stateCompress, final StateSnapshot.Writer<T> stateWriter, final T state)
                    throws IOException {
            SnapshotFileV1.createNew(file, timestamp, lastIncluded, votingConfig, entryCompress, unappliedEntries,
                stateCompress, stateWriter, state);
        }

        @Override
        public SnapshotFile open(final Path file) throws IOException {
            return SnapshotFileV1.open(file);
        }
    };

    private final String extension;

    SnapshotFileFormat(final String extension) {
        this.extension = requireNonNull(extension);
    }

    /**
     * Returns the extension associated with this file format.
     *
     * @return the extension associated with this file format
     */
    public final String extension() {
        return extension;
    }

    /**
     * Returns the latest format.
     *
     * @return the latest format
     */
    public static SnapshotFileFormat latest() {
        return SNAPSHOT_V1;
    }

    /**
     * Returns the {@link CompressionType} corresponding to specified file name by examining its extension.
     *
     * @param fileName the file name
     * @return the {@link CompressionType}, or {@code null} if the file extension is not recognized.
     */
    public static @Nullable SnapshotFileFormat forFileName(final String fileName) {
        for (var format : values()) {
            if (fileName.endsWith(format.extension())) {
                return format;
            }
        }
        return null;
    }

    /**
     * Create a file of this format with the content of specified snapshot.
     *
     * @param <T> state type
     * @param file the file to write
     * @param timestamp the timestamp
     * @param lastIncluded last journal entry included in the snapshot
     * @param votingConfig the server configuration
     * @param entryCompress the compression to apply to unapplied entries
     * @param unappliedEntries the unapplied entries
     * @param stateCompress the compression to apply to user state
     * @param stateWriter serialization support for the state type
     * @param state the state
     * @throws IOException if an I/O error occurs
     */
    public abstract <T extends StateSnapshot> void createNew(Path file, Instant timestamp, EntryInfo lastIncluded,
        @Nullable VotingConfig votingConfig, CompressionType entryCompress, List<ReplicatedLogEntry> unappliedEntries,
        CompressionType stateCompress, StateSnapshot.Writer<T> stateWriter, T state) throws IOException;

    /**
     * Open an existing file of this format.
     *
     * @param file the file to open
     * @return a {@link SnapshotFile}
     * @throws IOException if an I/O error occurs
     */
    public abstract SnapshotFile open(Path file) throws IOException;
}
