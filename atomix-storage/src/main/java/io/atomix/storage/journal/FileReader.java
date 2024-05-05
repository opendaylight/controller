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

import com.google.common.base.MoreObjects;
import java.nio.ByteBuffer;
import org.eclipse.jdt.annotation.NonNull;

/**
 * An abstraction over how to read a {@link JournalSegmentFile}.
 */
abstract sealed class FileReader permits DiskFileReader, MappedFileReader {
    private final JournalSegmentFile file;

    FileReader(final JournalSegmentFile file) {
        this.file = requireNonNull(file);
    }

    /**
     * Invalidate any cache that is present, so that the next read is coherent with the backing file.
     */
    abstract void invalidateCache();

    /**
     * Read the some bytes as specified position. The sum of position and size is guaranteed not to exceed the maximum
     * segment size nor maximum entry size.
     *
     * @param position position to the entry header
     * @param size to read
     * @return resulting buffer
     */
    abstract @NonNull ByteBuffer read(int position, int size);

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("path", file.path()).toString();
    }
}
