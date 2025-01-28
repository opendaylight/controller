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
import org.eclipse.jdt.annotation.NonNull;

/**
 * Log writer.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public interface JournalWriter<E> {
    /**
     * Returns the next index to be written.
     *
     * @return The next index to be written.
     */
    long getNextIndex();

    /**
     * Appends an entry to the journal.
     *
     * @param entry The entry to append.
     * @return The appended indexed entry.
     */
    <T extends E> @NonNull Indexed<T> append(T entry) throws IOException;

    /**
     * Commits entries up to the given index.
     *
     * @param index The index up to which to commit entries.
     */
    void commit(long index) throws IOException;

    /**
     * Resets the head of the journal to the given index.
     *
     * @param index the next index to write
     * @throws IndexOutOfBoundsException if the journal cannot be reset to specified index
     */
    void reset(long index) throws IOException;

    /**
     * Flushes written entries to disk.
     */
    void flush() throws IOException;
}
