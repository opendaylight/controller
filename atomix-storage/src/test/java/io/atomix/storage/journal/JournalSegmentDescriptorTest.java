/*
 * Copyright 2017-2022 Open Networking Foundation and others.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.storage.journal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Segment descriptor test.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
class JournalSegmentDescriptorTest {
    /**
     * Tests the segment descriptor builder.
     */
    @Test
    void testDescriptorBuilder() {
        final var descriptor = JournalSegmentDescriptor.builder()
            .withId(2)
            .withIndex(1025)
            .withMaxSegmentSize(1024 * 1024)
            .withMaxEntries(2048)
            .withUpdated(0)
            .build();

        assertEquals(2, descriptor.id());
        assertEquals(JournalSegmentDescriptor.VERSION, descriptor.version());
        assertEquals(1025, descriptor.index());
        assertEquals(1024 * 1024, descriptor.maxSegmentSize());
        assertEquals(2048, descriptor.maxEntries());
        assertEquals(0, descriptor.updated());
    }

    /**
     * Tests copying the segment descriptor.
     */
    @Test
    void testToArray() {
        assertArrayEquals(new byte[] {
            0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 4, 1, 0, 16, 0, 0, 0, 0, 8, 0, 8, 7, 6, 5,
            4, 3, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        }, JournalSegmentDescriptor.builder()
            .withId(2)
            .withIndex(1025)
            .withMaxSegmentSize(1024 * 1024)
            .withMaxEntries(2048)
            .withUpdated(0x0807060504030201L)
            .build()
            .toArray());
    }
}
