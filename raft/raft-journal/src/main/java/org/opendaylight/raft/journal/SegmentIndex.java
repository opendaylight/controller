/*
 * Copyright 2018-2022 Open Networking Foundation and others.  All rights reserved.
 * Copyright (c) 2024 PANTHEON.tech, s.r.o.
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
package org.opendaylight.raft.journal;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.raft.journal.Segment.Position;

/**
 * Index of a particular {@code Segment}.
 */
interface SegmentIndex {
    /**
     * Adds an entry for the given index at the given position.
     *
     * @param index the index for which to add the entry
     * @param position the position of the given index
     * @return A {@link Position}
     */
    @NonNull Position index(long index, int position);

    /**
     * Return the last position known to this index.
     *
     * @return the last position known to this index
     */
    @Nullable Position last();

    /**
     * Looks up the position of the given index.
     *
     * @param index the index to lookup
     * @return the position of the given index or a lesser index, or {@code null}
     */
    @Nullable Position lookup(long index);

    /**
     * Truncates the index to the given index and returns its position, if available.
     *
     * @param index the index to which to truncate the index, or {@code null}
     * @return the position of the given index or a lesser index, or {@code null}
     */
    @Nullable Position truncate(long index);
}
