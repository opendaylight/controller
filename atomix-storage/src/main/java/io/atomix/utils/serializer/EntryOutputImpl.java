/*
 * Copyright (c) 2023 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.utils.serializer;

import io.atomix.storage.journal.JournalSerdes;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class EntryOutputImpl implements JournalSerdes.EntryOutput {
    private final ObjectOutputStream stream;

    public EntryOutputImpl(final ObjectOutputStream stream) {
        this.stream = stream;
    }

    @Override
    public void writeBytes(final byte[] bytes) throws IOException {
        stream.write(bytes);
    }

    @Override
    public void writeLong(final long value) throws IOException {
        stream.writeLong(value);
    }

    @Override
    public void writeObject(final Object value) throws IOException {
        stream.writeObject(value);
    }

    @Override
    public void writeString(final String value) throws IOException {
        if (value == null) {
            //TODO: EntryInputImpl can't read null is it a problem?
            stream.writeUTF("");
            return;
        }
        stream.writeUTF(value);
    }

    @Override
    public void writeVarInt(final int value) throws IOException {
        stream.write(value);
    }
}
