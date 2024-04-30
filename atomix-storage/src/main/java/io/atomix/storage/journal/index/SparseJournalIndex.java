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

import java.util.TreeMap;

/**
 * A {@link JournalIndex} maintaining target density.
 */
public final class SparseJournalIndex implements JournalIndex {
    private static final int MIN_DENSITY = 1000;

    private final int density;
    private final TreeMap<Long, Integer> positions = new TreeMap<>();

    public SparseJournalIndex() {
        density = MIN_DENSITY;
    }

    public SparseJournalIndex(final double density) {
        this.density = (int) Math.ceil(MIN_DENSITY / (density * MIN_DENSITY));
    }

    @Override
    public void index(final long index, final int position) {
        if (index % density == 0) {
            positions.put(index, position);
        }
    }

    @Override
    public Position lookup(final long index) {
        return Position.ofNullable(positions.floorEntry(index));
    }

    @Override
    public Position truncate(final long index) {
        positions.tailMap(index, false).clear();
        return Position.ofNullable(positions.lastEntry());
    }

    @Override
    public String toString() {
        return String.valueOf(positions);
    }
}
