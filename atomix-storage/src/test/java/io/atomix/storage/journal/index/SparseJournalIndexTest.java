/*
 * Copyright 2018-2022 Open Networking Foundation and others.  All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Sparse journal index test.
 */
class SparseJournalIndexTest {
    private final SparseJournalIndex sparseIndex = new SparseJournalIndex(.2);

    @Test
    void firstTest() throws Exception {
        assertNull(sparseIndex.lookup(1));
        assertIndex(1, 2);
        assertNull(sparseIndex.lookup(1));
        assertIndex(2, 4);
        assertIndex(3, 6);
        assertIndex(4, 8);
        assertIndex(5, 10);
        assertEquals(new Position(5, 10), sparseIndex.lookup(5));
        assertIndex(6, 12);
        assertIndex(7, 14);
        assertIndex(8, 16);
        assertEquals(new Position(5, 10), sparseIndex.lookup(8));
        assertIndex(9, 18);
        assertIndex(10, 20);
        assertEquals(new Position(10, 20), sparseIndex.lookup(10));
        assertEquals(new Position(5, 10), sparseIndex.truncate(8));
        assertEquals(new Position(5, 10), sparseIndex.lookup(5));
        assertEquals(new Position(5, 10), sparseIndex.lookup(8));
        assertEquals(new Position(5, 10), sparseIndex.lookup(10));
        assertEquals(new Position(5, 10), sparseIndex.truncate(5));
        assertNull(sparseIndex.lookup(5));
        assertNull(sparseIndex.lookup(8));
        assertNull(sparseIndex.truncate(4));
        assertNull(sparseIndex.lookup(4));
        assertNull(sparseIndex.lookup(8));
    }

    @Test
    void secondTest() {
        assertNull(sparseIndex.lookup(100));
        assertIndex(101, 2);
        assertNull(sparseIndex.lookup(1));
        assertIndex(102, 4);
        assertIndex(103, 6);
        assertIndex(104, 8);
        assertIndex(105, 10);
        assertEquals(new Position(105, 10), sparseIndex.lookup(105));
        assertIndex(106, 12);
        assertIndex(107, 14);
        assertIndex(108, 16);
        assertEquals(new Position(105, 10), sparseIndex.lookup(108));
        assertIndex(109, 18);
        assertIndex(110, 20);
        assertEquals(new Position(110, 20), sparseIndex.lookup(110));
        assertEquals(new Position(105, 10), sparseIndex.truncate(108));
        assertEquals(new Position(105, 10), sparseIndex.lookup(108));
        assertEquals(new Position(105, 10), sparseIndex.lookup(110));
        assertNull(sparseIndex.truncate(104));
        assertNull(sparseIndex.lookup(104));
        assertNull(sparseIndex.lookup(108));
    }

    private void assertIndex(final long index, final int position) {
        assertEquals(new Position(index, position), sparseIndex.index(index, position));
    }
}
