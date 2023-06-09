/*
 * Copyright (c) 2023 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.utils.serializer;

import io.atomix.storage.journal.JournalSerdes;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

public class JournalSerdesImpl implements JournalSerdes {
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    // For backward compatibility we start from 18
    static final byte INITIAL_ID = 18;
    static final byte JOURNAL_VERSION = 2;
    static final byte LEGACY_VERSION = 1;
    private final Map<Byte, RegisteredType> registeredTypes;

    JournalSerdesImpl(final Map<Byte, RegisteredType> registeredTypes) {
        this.registeredTypes = Map.copyOf(registeredTypes);
    }
    @Override
    public byte[] serialize(final Object obj) {
        try (final var out = new ByteArrayOutputStream()) {
            serialize(obj, out);
            return out.toByteArray();
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
        buffer.put(type.getKey());
        // Indicator we sre using new EntryInput without kryo
        buffer.put(JOURNAL_VERSION);

        try (final var bos = new ByteArrayOutputStream()) {
            try (final var out = new ObjectOutputStream(bos)) {
                type.getValue().serializer().write(new EntryOutputImpl(out), obj);
            }
            buffer.put(bos.toByteArray());
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
        final var header = new byte[2];
        header[0] = type.getKey();
        // Indicator we sre using new EntryInput without kryo
        header[1] = JOURNAL_VERSION;

        try (final var out = new ObjectOutputStream(stream)) {
            out.write(header);
            type.getValue().serializer().write(new EntryOutputImpl(out), obj);
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

        if (legacyTag == JOURNAL_VERSION) {
            try (final var byteBufferBackedInputStream = new ByteBufferBackedInputStream(buffer)) {
                try (final var input = new ObjectInputStream(byteBufferBackedInputStream)) {
                    return (T) type.serializer().read(new EntryInputImpl(input));
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        if (legacyTag == LEGACY_VERSION) {
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

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(final InputStream stream, final int bufferSize) {
        final var header = new byte[2];
        try {
            if (stream.read(header) != 2) {
                throw new IllegalStateException("Invalid serialization format");
            }
            final var type = findRegisteredType(header[0]);

            if (header[1] == JOURNAL_VERSION) {
                try (final var input = new ObjectInputStream(stream)) {
                    return (T) type.serializer().read(new EntryInputImpl(input));
                }
            }
            if (header[1] == LEGACY_VERSION) {
                //TODO: finish implementation if needed
                throw new UnsupportedOperationException();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalStateException("Invalid serialization format");
    }

    private Map.Entry<Byte, RegisteredType> findRegisteredType(final Object obj) {
        for (final var entry : registeredTypes.entrySet()) {
            final var classes = entry.getValue().types();
            for (final var clazz : classes) {
                if (obj.getClass() == clazz){
                    return entry;
                }
            }
        }
        throw new IllegalStateException(String.format("No serializer registered for given object: %s", obj));
    }

    private RegisteredType findRegisteredType(final byte id) {
        final var type = registeredTypes.get(id);
        if (type == null) {
            throw new IllegalStateException(String.format("No serializer registered for given id: %s", id));
        }
        return type;
    }

    static final class ByteBufferBackedInputStream extends InputStream {
        final ByteBuffer buf;

        ByteBufferBackedInputStream(final ByteBuffer buf) {
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
