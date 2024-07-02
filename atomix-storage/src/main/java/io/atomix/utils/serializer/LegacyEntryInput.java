/*
 * Copyright (c) 2023 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.utils.serializer;

import static java.util.Objects.requireNonNull;

import io.atomix.storage.journal.JournalSerdes;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;

final class LegacyEntryInput implements JournalSerdes.EntryInput {
    private final Kryo505ByteBufferInput input;

    LegacyEntryInput(final ByteBuffer input) {
        requireNonNull(input);
        this.input = new Kryo505ByteBufferInput(input);
    }

    @Override
    public byte[] readBytes(final int length) throws IOException {
        final byte[] output = new byte[length];
        input.read(output);
        return output;
    }

    @Override
    public long readLong() {
        //TODO: finish implementation if needed
        throw new UnsupportedOperationException();
    }

    @Override
    public String readString() {
        return input.readString();
    }

    @Override
    public Object readObject() throws IOException {
        try (var inputStream = new ObjectInputStream(this.input)) {
            return inputStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int readVarInt() throws IOException {
        return input.readVarInt(true);
    }
}
