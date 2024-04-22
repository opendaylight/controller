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
import org.eclipse.jdt.annotation.Nullable;

/**
 * A reader of {@link ByteBufJournal} entries.
 */
@NonNullByDefault
public interface ByteBufReader extends AutoCloseable {
    /**
     * A journal entry processor. Responsible for transforming bytes into their internal representation.
     *
     * @param <T> Internal representation type
     */
    @FunctionalInterface
    interface EntryMapper<T> {
        /**
         * Process an entry.
         *
         * @param index entry index
         * @param bytes entry bytes
         * @return resulting internal representation
         */
        T mapEntry(long index, ByteBuf bytes);
    }

    /**
     * Returns the first index in the journal.
     *
     * @return The first index in the journal
     */
    long firstIndex();

    /**
     * Returns the next reader index.
     *
     * @return The next reader index
     */
    long nextIndex();

    /**
     * Try to move to the next binary data block
     *
     * @param mapper callback to be invoked on binary data
     * @return processed binary data, or {@code null}
     */
    <T> @Nullable T tryNext(EntryMapper<T> mapper);

    /**
     * Resets the reader to the start.
     */
    void reset();

    /**
     * Resets the reader to the given index.
     *
     * @param index The index to which to reset the reader
     */
    void reset(long index);

    @Override
    void close();
}
