/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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
