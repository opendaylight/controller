/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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
    public void write(final EntryOutput output, final byte[] entry) throws IOException {
        if (entry != null) {
            output.writeVarInt(entry.length + 1);
            output.writeBytes(entry);
        } else {
            output.writeVarInt(0);
        }
    }
}
