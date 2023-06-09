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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class JournalSerdesImpl implements JournalSerdes {
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    // For backward compatibility we start from 18
    static final byte INITIAL_ID = 18;
    private final List<RegisteredType> registeredTypes;

    JournalSerdesImpl(final List<RegisteredType> registeredTypes) {
        this.registeredTypes = List.copyOf(registeredTypes);
    }
    @Override
    public byte[] serialize(final Object obj) {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            serialize(obj, b);
            return b.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public byte[] serialize(final Object obj, final int bufferSize) {
        final var buffer = ByteBuffer.allocate(bufferSize);
        serialize(obj, buffer);
        return buffer.array();
    }

    @Override
    public void serialize(final Object obj, final ByteBuffer buffer) {
        final var type = findRegisteredType(obj);
        buffer.put(type.id());
        // Indicator we sre using new EntryInput without kryo
        buffer.put((byte) 2);

        try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            try (ObjectOutputStream o = new ObjectOutputStream(b)) {
                type.serializer().write(new EntryOutputImpl(o), obj);
            }
            buffer.put(b.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void serialize(final Object obj, final OutputStream stream) {
        serialize(obj, stream, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public void serialize(final Object obj, final OutputStream stream, final int bufferSize) {
        final var type = findRegisteredType(obj);
        final byte[] header = new byte[2];
        header[0] = type.id();
        // Indicator we sre using new EntryInput without kryo
        header[1] = (byte) 2;

        try (ObjectOutputStream o = new ObjectOutputStream(stream)) {
            o.write(header);
            type.serializer().write(new EntryOutputImpl(o), obj);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <T> T deserialize(final byte[] bytes) {
        return deserialize(new ByteArrayInputStream(bytes));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(final ByteBuffer buffer) {
        final byte id = buffer.get();
        final var type = findRegisteredType(id);
        final byte legacyTag = buffer.get();

        if (legacyTag == 2) {
            try (var byteBufferBackedInputStream = new ByteBufferBackedInputStream(buffer)) {
                try (ObjectInputStream o = new ObjectInputStream(byteBufferBackedInputStream)) {
                    return (T) type.serializer().read(new EntryInputImpl(o));
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        if (legacyTag == 1) {
            try {
                return (T) type.serializer().read(new LegacyEntryInput(buffer));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("Invalid serialization format");
    }

    @Override
    public <T> T deserialize(final InputStream stream) {
        return deserialize(stream, DEFAULT_BUFFER_SIZE);
    }

    @SuppressFBWarnings("RR_NOT_CHECKED")
    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(final InputStream stream, final int bufferSize) {
        final byte[] header = new byte[2];
        try {
            stream.read(header);
            final var type = findRegisteredType(header[0]);

            if (header[1] == 2) {
                try (ObjectInputStream o = new ObjectInputStream(stream)) {
                    return (T) type.serializer().read(new EntryInputImpl(o));
                }
            }
            if (header[1] == 1) {
                //TODO: finish implementation if needed
                throw new UnsupportedOperationException();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalStateException("Invalid serialization format");
    }

    private RegisteredType findRegisteredType(final Object obj) {
        for (var type : registeredTypes) {
            final var classes = type.types();
            for (var clazz : classes) {
                if (obj.getClass() == clazz){
                    return type;
                }
            }
        }
        throw new IllegalStateException(String.format("No serializer registered for given object: %s", obj));
    }

    private RegisteredType findRegisteredType(final byte id) {
        for (var type : registeredTypes) {
            if (type.id() == id) {
                return type;
            }
        }
        throw new IllegalStateException(String.format("No serializer registered for given id: %s", id));
    }

    static final class ByteBufferBackedInputStream extends InputStream {
        final ByteBuffer buf;

        ByteBufferBackedInputStream(ByteBuffer buf) {
            this.buf = buf;
        }

        public synchronized int read() throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get();
        }

        public synchronized int read(final byte[] bytes, final int off, int len) throws IOException {
            len = Math.min(len, buf.remaining());
            buf.get(bytes, off, len);
            return len;
        }
    }
}
