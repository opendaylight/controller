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
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import io.atomix.storage.journal.JournalSerdes.EntryOutput;
import java.io.IOException;

final class KryoEntryOutput implements EntryOutput {
    private final Kryo kryo;
    private final Output output;
    private final JavaSerializer javaSerializer;

    KryoEntryOutput(final Kryo kryo, final Output output, final JavaSerializer javaSerializer) {
        this.kryo = requireNonNull(kryo);
        this.output = requireNonNull(output);
        this.javaSerializer = requireNonNull(javaSerializer);
    }

    @Override
    public void writeBytes(final byte[] bytes) throws IOException {
        try {
            output.writeBytes(bytes);
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeLong(final long value) throws IOException {
        try {
            output.writeLong(value, false);
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeObject(final Object value) throws IOException {
        try {
            javaSerializer.write(kryo, output, value);
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeString(final String value) throws IOException {
        try {
            output.writeString(value);
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeVarInt(final int value) throws IOException {
        try {
            output.writeVarInt(value, true);
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }
}
