package io.atomix.utils.serializer;

import io.atomix.storage.journal.JournalSerdes;

import java.io.IOException;
import java.io.ObjectOutputStream;

public class EntryInputImpl implements JournalSerdes.EntryInput {
    private final ObjectOutputStream stream;

    public EntryInputImpl(ObjectOutputStream stream) {
        this.stream = stream;
    }

    @Override
    public byte[] readBytes(int length) throws IOException {
        return new byte[0];
    }

    @Override
    public long readLong() throws IOException {
        return 0;
    }

    @Override
    public String readString() throws IOException {
        return null;
    }

    @Override
    public Object readObject() throws IOException {
        return null;
    }

    @Override
    public int readVarInt() throws IOException {
        return 0;
    }
}
