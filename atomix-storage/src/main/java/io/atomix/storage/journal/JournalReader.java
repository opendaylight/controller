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

import org.eclipse.jdt.annotation.Nullable;

/**
 * Log reader.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
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
     * Returns the first index in the journal.
     *
     * @return the first index in the journal
     */
    long getFirstIndex();

    /**
     * Returns the next reader index.
     *
     * @return The next reader index.
     */
    long getNextIndex();

    /**
     * Try to move to the next entry.
     *
     * @return The next entry in the reader, or {@code null} if there is no next entry.
     */
    @Nullable Indexed<E> tryNext();

    /**
     * Resets the reader to the start.
     */
    void reset();

    /**
     * Resets the reader to the given index.
     *
     * @param index The index to which to reset the reader.
     */
    void reset(long index);

    @Override
    void close();
}
