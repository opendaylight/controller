package io.atomix.utils.serializer;

import com.sun.jdi.request.InvalidRequestStateException;
import io.atomix.storage.journal.JournalSerdes;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class JournalSerdesImpl implements JournalSerdes {
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    static final String NO_NAME = "(no name)";
    // For backward compatibility we start from 18
    static final byte INITIAL_ID = 18;
    private final List<RegisteredType> registeredTypes;
    private final String friendlyName;

    JournalSerdesImpl(
            final List<RegisteredType> registeredTypes, final String friendlyName) {
        this.registeredTypes = List.copyOf(registeredTypes);
        this.friendlyName = requireNonNull(friendlyName);
    }
    @Override
    public byte[] serialize(Object obj) {
        return serialize(obj, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public byte[] serialize(Object obj, int bufferSize) {
        var buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        serialize(obj, buffer);
        return buffer.array();
    }

    @Override
    public void serialize(Object obj, ByteBuffer buffer) {
        var type = findRegisteredType(obj);
        buffer.put(type.id());
        // Indicator we sre using new EntryInput without kryo
        buffer.put((byte) 2);

        try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            try (ObjectOutputStream o = new ObjectOutputStream(b)) {
                type.serializer().write(new EntryOutputImpl(o), obj);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            buffer.put(b.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void serialize(Object obj, OutputStream stream) {

    }

    @Override
    public void serialize(Object obj, OutputStream stream, int bufferSize) {

    }

    @Override
    public <T> T deserialize(byte[] bytes) {
        return null;
    }

    @Override
    public <T> T deserialize(ByteBuffer buffer) {
        return null;
    }

    @Override
    public <T> T deserialize(InputStream stream) {
        return null;
    }

    @Override
    public <T> T deserialize(InputStream stream, int bufferSize) {
        return null;
    }

    private RegisteredType findRegisteredType(Object obj) {
        for (var type : registeredTypes) {
            var classes = type.types();
            for (var clazz : classes) {
                if (obj.getClass() == clazz){
                    return type;
                }
            }
        }
        throw new InvalidRequestStateException(String.format("No serializer registered for given object: %s", obj));
    }

    static final class ByteBufferBackedInputStream extends InputStream {
        ByteBuffer buf;

        ByteBufferBackedInputStream(ByteBuffer buf) {
            this.buf = buf;
        }

        public synchronized int read() throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get();
        }

        public synchronized int read(byte[] bytes, int off, int len) throws IOException {
            len = Math.min(len, buf.remaining());
            buf.get(bytes, off, len);
            return len;
        }
    }
}
