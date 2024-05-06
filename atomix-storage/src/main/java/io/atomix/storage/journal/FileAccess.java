/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
