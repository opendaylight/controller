/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Enumeration of file formats we support for {@link SnapshotSource}.
 */
@NonNullByDefault
public enum SnapshotFileFormat {
    /**
     * A plain file.
     */
    PLAIN(".plain") {
        @Override
        public FilePlainSnapshotSource sourceForFile(final Path path) {
            return new FilePlainSnapshotSource(path);
        }
    },
    /**
     * A plain file.
     */
    LZ4(".lz4") {
        @Override
        public FileLz4SnapshotSource sourceForFile(final Path path) {
            return new FileLz4SnapshotSource(path);
        }
    };

    // Note: starts with ".", to make operations easier
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
     * Create a new {@link SnapshotSource} backed by specified file path.
     *
     * @param path the path
     * @return a {@link SnapshotSource}
     */
    public abstract SnapshotSource sourceForFile(Path path);

    /**
     * Returns the {@link SnapshotFileFormat} corresponding to specified file name by examining its extension.
     *
     * @param fileName the file name
     * @return the {@link SnapshotFileFormat}, or {@code null} if the file extension is not recognized.
     */
    public static @Nullable SnapshotFileFormat forFileName(final String fileName) {
        for (var format : values()) {
            if (fileName.endsWith(format.extension)) {
                return format;
            }
        }
        return null;
    }
}