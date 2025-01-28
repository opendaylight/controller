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
import org.eclipse.jdt.annotation.Nullable;

/**
 * A reader of {@link RaftJournal} entries.
 */
@NonNullByDefault
public interface EntryReader extends AutoCloseable {
    /**
     * Returns the next reader index.
     *
     * @return The next reader index
     */
    long nextIndex();

    /**
     * Try to move to the next binary data block.
     *
     * @param <T> Internal representation type
     * @param mapper callback to be invoked on binary data
     * @return processed binary data, or {@code null}
     */
    <T> @Nullable T tryNext(FromByteBufMapper<T> mapper);

    /**
     * Resets the reader to the start.
     */
    void reset();

    /**
     * Resets the reader to the given index.
     *
     * @param index the next index to read
     */
    void reset(long index);

    @Override
    void close();
}
