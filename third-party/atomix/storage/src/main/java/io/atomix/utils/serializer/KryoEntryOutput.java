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
