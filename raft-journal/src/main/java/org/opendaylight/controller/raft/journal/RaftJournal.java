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
package org.opendaylight.controller.raft.journal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A journal of byte arrays. Provides the ability to write modify entries via {@link EntryWriter} and read them
 * back via {@link EntryReader}.
 */
@NonNullByDefault
public interface RaftJournal extends AutoCloseable {
    /**
     * Return the index of the first entry in the journal.
     *
     * @return the index of the first entry in the journal
     */
    long firstIndex();

    /**
     * Return the index of the last entry in the journal.
     *
     * @return the last index, or zero if there are no entries
     */
    long lastIndex();

    /**
     * Returns the journal writer.
     *
     * @return The journal writer.
     */
    EntryWriter writer();

    /**
     * Opens a new {@link EntryReader} reading all entries.
     *
     * @param index The index at which to start the reader.
     * @return A new journal reader.
     */
    EntryReader openReader(long index);

    /**
     * Opens a new {@link EntryReader} reading only committed entries.
     *
     * @param index The index at which to start the reader.
     * @return A new journal reader.
     */
    EntryReader openCommitsReader(long index);

    /**
     * Compacts the journal up to the given index. The semantics of compaction are not specified by this interface.
     *
     * @param index The index up to which to compact the journal.
     */
    void compact(long index);

    @Override
    void close();
}
