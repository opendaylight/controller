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
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.google.common.base.MoreObjects;
import io.atomix.storage.journal.JournalSerdes.EntrySerdes;
import java.io.IOException;

final class EntrySerializer<T> extends Serializer<T> {
    // Note: uses identity to create things in Kryo, hence we want an instance for every serdes we wrap
    private final JavaSerializer javaSerializer = new JavaSerializer();
    private final EntrySerdes<T> serdes;

    EntrySerializer(final EntrySerdes<T> serdes) {
        this.serdes = requireNonNull(serdes);
    }

    @Override
    public T read(final Kryo kryo, final Input input, final Class<T> type) {
        try {
            return serdes.read(new KryoEntryInput(kryo, input, javaSerializer));
        } catch (IOException e) {
            throw new KryoException(e);
        }
    }

    @Override
    public void write(final Kryo kryo, final Output output, final T object) {
        try {
            serdes.write(new KryoEntryOutput(kryo, output, javaSerializer), object);
        } catch (IOException e) {
            throw new KryoException(e);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).addValue(serdes).toString();
    }
}
