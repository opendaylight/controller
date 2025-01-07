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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Journal segment file test.
 */
class JournalSegmentFileTest {
    @Test
    void testIsSegmentFile() {
        assertTrue(JournalSegmentFile.isSegmentFile("foo", "foo-1.log"));
        assertFalse(JournalSegmentFile.isSegmentFile("foo", "bar-1.log"));
        assertTrue(JournalSegmentFile.isSegmentFile("foo", "foo-1-1.log"));
    }

    @Test
    void testCreateSegmentFile() {
        final var file = JournalSegmentFile.createSegmentFile("foo", Path.of(System.getProperty("user.dir")), 1);
        assertTrue(JournalSegmentFile.isSegmentFile("foo", file));
    }
}
