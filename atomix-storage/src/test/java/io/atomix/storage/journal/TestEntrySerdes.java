/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
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

import io.atomix.storage.journal.JournalSerdes.EntryInput;
import io.atomix.storage.journal.JournalSerdes.EntryOutput;
import io.atomix.storage.journal.JournalSerdes.EntrySerdes;
import java.io.IOException;

final class TestEntrySerdes implements EntrySerdes<TestEntry> {
    private static final ByteArraySerdes BA_SERIALIZER = new ByteArraySerdes();

    @Override
    public TestEntry read(final EntryInput input) throws IOException {
        return new TestEntry(BA_SERIALIZER.read(input));
    }

    @Override
    public void write(final EntryOutput output, final TestEntry entry) throws IOException {
        BA_SERIALIZER.write(output, entry.bytes());
    }
}
