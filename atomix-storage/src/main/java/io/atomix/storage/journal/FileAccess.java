/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Abstract base class for accessing a particular file.
 */
@NonNullByDefault
abstract sealed class FileAccess implements AutoCloseable permits DiskFileAccess, MappedFileAccess {
    final JournalSegmentFile file;
    final int maxEntrySize;

    FileAccess(final JournalSegmentFile file, final int maxEntrySize) {
        this.file = requireNonNull(file);
        this.maxEntrySize = maxEntrySize;
    }

    /**
     * Create a new {@link FileReader}.
     *
     * @return a new {@link FileReader}
     */
    abstract FileReader newFileReader();

    /**
     * Create a new {@link FileWriter}.
     *
     * @return a new {@link FileWriter}
     */
    abstract FileWriter newFileWriter();

    @Override
    public abstract void close();
}
