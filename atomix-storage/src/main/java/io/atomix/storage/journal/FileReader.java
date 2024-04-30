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

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.Nullable;

/**
 * An abstraction over how to read a {@link JournalSegmentFile}.
 */
sealed interface FileReader permits DiskFileReader, MappedFileReader {

    /**
     * Invalidate any cache that is present, so that the next read is coherent with the backing file.
     */
    void invalidateCache();

    /**
     * Reads the entry from specified position. First reads the entry header (entry length and checksum),
     * then the entry itself. Returns the entry content as {@link ByteBuf} object if the header contains
     * non-zero length and header checksum matches calculated one, null otherwise.
     *
     * @param position position to the entry header
     * @return entry content as {@link ByteBuf} or null
     */
    @Nullable ByteBuf read(int position);
}
