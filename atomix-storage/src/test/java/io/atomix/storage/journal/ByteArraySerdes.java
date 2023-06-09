/*
 * Copyright 2023 PANTHEON.tech, s.r.o.
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

final class ByteArraySerdes implements EntrySerdes<byte[]> {
    @Override
    public byte[] read(final EntryInput input) throws IOException {
        int length = input.readVarInt();
        return length == 0 ? null : input.readBytes(length - 1);
    }

    @Override
    public void write(final EntryOutput output, final Object entry) throws IOException {
        byte[] bytes = (byte[]) entry;
        if (entry != null) {
            output.writeVarInt(bytes.length + 1);
            output.writeBytes(bytes);
        } else {
            output.writeVarInt(0);
        }
    }
}
