package io.atomix.utils.serializer;

import io.atomix.storage.journal.JournalSerdes;

import java.io.IOException;
import java.io.ObjectOutputStream;

public class EntryOutputImpl implements JournalSerdes.EntryOutput {
    private final ObjectOutputStream stream;

    public EntryOutputImpl(ObjectOutputStream stream) {
        this.stream = stream;
    }

    @Override
    public void writeBytes(byte[] bytes) throws IOException {
        stream.write(bytes);
    }

    @Override
    public void writeLong(long value) throws IOException {
        stream.writeLong(value);
    }

    @Override
    public void writeObject(Object value) throws IOException {
        stream.writeObject(value);
    }

    @Override
    public void writeString(String value) throws IOException {
        stream.writeObject(value);
    }

    @Override
    public void writeVarInt(int value) throws IOException {
        stream.write(value);
    }
}
