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
package io.atomix.utils.serializer;

import static java.util.Objects.requireNonNull;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import io.atomix.storage.journal.JournalSerdes.EntryInput;
import java.io.IOException;

final class KryoEntryInput implements EntryInput {
    private final Kryo kryo;
    private final Input input;
    private final JavaSerializer javaSerializer;

    KryoEntryInput(final Kryo kryo, final Input input, final JavaSerializer javaSerializer) {
        this.kryo = requireNonNull(kryo);
        this.input = requireNonNull(input);
        this.javaSerializer = requireNonNull(javaSerializer);
    }

    @Override
    public byte[] readBytes(final int length) throws IOException {
        try {
            return input.readBytes(length);
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    @Override
    public long readLong() throws IOException {
        try {
            return input.readLong(false);
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Object readObject() throws IOException {
        try {
            return javaSerializer.read(kryo, input, null);
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String readString() throws IOException {
        try {
            return input.readString();
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int readVarInt() throws IOException {
        try {
            return input.readVarInt(true);
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }
}
