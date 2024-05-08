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
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A writer of {@link ByteBufJournal} entries.
 */
@NonNullByDefault
public interface ByteBufWriter {
    /**
     * Returns the next index to be written.
     *
     * @return The next index to be written
     */
    long nextIndex();

    /**
     * Appends an entry to the journal.
     *
     * @param bytes Data block to append
     * @return The index of appended data block
     */
    // FIXME: throws IOException
    long append(ByteBuf bytes);

    /**
     * Commits entries up to the given index.
     *
     * @param index The index up to which to commit entries.
     */
    void commit(long index);

    /**
     * Resets the head of the journal to the given index.
     *
     * @param index the next index to write
     * @throws IndexOutOfBoundsException if the journal cannot be reset to specified index
     */
    // FIXME: throws IOException
    void reset(long index);

    /**
     * Flushes written entries to disk.
     */
    // FIXME: throws IOException
    void flush();
}
