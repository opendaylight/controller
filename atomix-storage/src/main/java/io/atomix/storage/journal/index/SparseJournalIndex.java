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
package io.atomix.storage.journal.index;

import com.google.common.base.MoreObjects;
import java.util.TreeMap;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A {@link JournalIndex} maintaining target density.
 */
public final class SparseJournalIndex implements JournalIndex {
    private static final int MIN_DENSITY = 1000;

    private final TreeMap<Long, Integer> positions = new TreeMap<>();
    private final int density;

    // Last known position. May not be accurate immediately after a truncate() or construction
    private @Nullable Position last;

    public SparseJournalIndex() {
        density = MIN_DENSITY;
    }

    public SparseJournalIndex(final double density) {
        this.density = (int) Math.ceil(MIN_DENSITY / (density * MIN_DENSITY));
    }

    @Override
    public Position index(final long index, final int position) {
        final var newLast = new Position(index, position);
        last = newLast;
        if (index % density == 0) {
            positions.put(index, position);
        }
        return newLast;
    }

    @Override
    public Position last() {
        return last;
    }

    @Override
    public Position lookup(final long index) {
        return Position.ofNullable(positions.floorEntry(index));
    }

    @Override
    public Position truncate(final long index) {
        positions.tailMap(index, false).clear();

        // Update last position to the last entry
        final var newLast = Position.ofNullable(positions.lastEntry());
        last = newLast;
        return newLast;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("positions", positions).toString();
    }
}
