/*
 * Copyright 2017-2022 Open Networking Foundation and others.  All rights reserved.
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

import java.io.IOException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Log reader.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
@NonNullByDefault
public interface JournalReader<E> extends AutoCloseable {
    /**
     * Raft log reader mode.
     */
    enum Mode {
        /**
         * Reads all entries from the log.
         */
        ALL,
        /**
         * Reads committed entries from the log.
         */
        COMMITS,
    }

    /**
     * A journal entry processor. Responsible for transforming entries into their internal representation.
     *
     * @param <E> Entry type
     * @param <T> Internal representation type
     */
    @FunctionalInterface
    interface EntryMapper<E, T> {
        /**
         * Process an entry.
         *
         * @param index entry index
         * @param entry entry itself
         * @param size entry size
         * @return resulting internal representation
         */
        T mapEntry(long index, E entry, int size);
    }

    /**
     * Returns the next reader index.
     *
     * @return The next reader index.
     */
    long getNextIndex();

    /**
     * Try to move to the next entry.
     *
     * @param entryMapper callback to be invoked for the entry
     * @return processed entry, or {@code null}
     */
    <T> @Nullable T tryNext(EntryMapper<E, T> entryMapper) throws IOException;

    /**
     * Resets the reader to the start.
     */
    void reset() throws IOException;

    /**
     * Resets the reader to the given index.
     *
     * @param index the next index to read
     */
    void reset(long index) throws IOException;

    @Override
    void close();
}
