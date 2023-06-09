/*
 * Copyright (c) 2023 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.utils.serializer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.atomix.storage.journal.JournalSerdes;
import java.io.IOException;
import java.io.ObjectInputStream;

public class EntryInputImpl implements JournalSerdes.EntryInput {
    private final ObjectInputStream stream;

    public EntryInputImpl(final ObjectInputStream stream) {
        this.stream = stream;
    }

    @SuppressFBWarnings("RR_NOT_CHECKED")
    @Override
    public byte[] readBytes(final int length) throws IOException {
        final byte[] output = new byte[length];
        stream.read(output);
        return output;
    }

    @Override
    public long readLong() throws IOException {
        return stream.readLong();
    }

    @Override
    public String readString() throws IOException {
        try {
            return (String) stream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Object readObject() throws IOException {
        try {
            return stream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int readVarInt() throws IOException {
        return stream.read();
    }
}
