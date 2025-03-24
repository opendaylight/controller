/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link EnabledRaftStorage} backed by a local directory.
 */
@NonNullByDefault
public final class LocalRaftStorage extends EnabledRaftStorage {
    @Beta
    public record Configuration(boolean useLZ4) {
        // Nothing else
    }

    /**
     * Enumeration of file formats we support.
     */
    private enum FileFormat {
        /**
         * A plain file.
         */
        PLAIN(".plain") {
            @Override
            FilePlainSnapshotSource sourceForFile(final Path path) {
                return new FilePlainSnapshotSource(path);
            }
        },
        /**
         * A plain file.
         */
        LZ4(".lz4") {
            @Override
            FileLz4SnapshotSource sourceForFile(final Path path) {
                return new FileLz4SnapshotSource(path);
            }
        };

        // Note: starts with ".", to make operations easier
        private final String extension;

        FileFormat(final String extension) {
            this.extension = requireNonNull(extension);
        }

        /**
         * Check if a file path matches this format and return the corresponding {@link FileSnapshotSource}.
         *
         * @param path path to check
         * @return a {@link FileSnapshotSource}, or {@null} if the path is not handled by this format
         */
        abstract FileSnapshotSource sourceForFile(Path path);

        static @Nullable FileFormat forFileName(final Path path) {
            for (var format : values()) {
                if (path.endsWith(format.extension)) {
                    return format;
                }
            }
            return null;
        }
    }

    private record SupportedFile(Path path, FileFormat format) {
        SupportedFile {
            requireNonNull(path);
            requireNonNull(format);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LocalRaftStorage.class);

    private final FileFormat format;
    private final String memberId;
    private final Path directory;

    public LocalRaftStorage(final String memberId, final Path directory, final Configuration config) {
        this.memberId = requireNonNull(memberId);
        this.directory = requireNonNull(directory);
        format = config.useLZ4 ? FileFormat.LZ4 : FileFormat.PLAIN;
    }

    @Override
    protected String memberId() {
        return memberId;
    }

    @Override
    public @Nullable SnapshotSource findLatestSnapshot() throws IOException {
        if (!Files.exists(directory)) {
                LOG.debug("{}: directory {} does not exist", memberId, directory);
            return null;
        }

        final List<SupportedFile> files;
        try (var paths = Files.list(directory)) {
            files = paths.map(this::pathToFile).filter(Objects::nonNull).toList();
        }

        if (files.isEmpty()) {
            LOG.debug("{}: no eligible files found", memberId);
            return null;
        }

        // FIXME: look at the sources and pick the latest one
        throw new UnsupportedOperationException();
    }

    private @Nullable SupportedFile pathToFile(final Path path) {
        if (!Files.isRegularFile(path)) {
            LOG.debug("{}: skipping non-file {}", memberId, path);
            return null;
        }
        if (!Files.isReadable(path)) {
            LOG.debug("{}: skipping unreadable file {}", memberId, path);
            return null;
        }
        final var name = path.getFileName();
        final var format = FileFormat.forFileName(name);
        if (format == null) {
            LOG.debug("{}: skipping unhandled file {}", memberId, path);
            return null;
        }

        // FIXME: strip extension and parse metadata

        LOG.debug("{}: using {} to handle file {}", memberId, format, path);
        return new SupportedFile(path, format);
    }

    @Override
    public <T> void persist(final T entry, final Consumer<T> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> void persistAsync(final T entry, final Consumer<T> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveSnapshot(final Snapshot snapshot) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSnapshots(final SnapshotSelectionCriteria criteria) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteMessages(final long sequenceNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLastSequenceNumber() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean handleJournalResponse(final JournalProtocol.Response response) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean handleSnapshotResponse(final SnapshotProtocol.Response response) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ToStringHelper addToStringAtrributes(final ToStringHelper helper) {
        return super.addToStringAtrributes(helper).add("directory", directory).add("format", format);
    }
}
