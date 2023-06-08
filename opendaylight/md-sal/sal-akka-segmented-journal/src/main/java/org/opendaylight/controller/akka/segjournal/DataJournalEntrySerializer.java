/*
 * Copyright (c) 2019 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorSystem;
import akka.actor.ExtendedActorSystem;
import akka.persistence.PersistentRepr;
import io.atomix.storage.journal.JournalSerdes;
import io.atomix.storage.journal.JournalSerdes.EntrySerdes;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.FromPersistence;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.ToPersistence;

/**
 * Kryo serializer for {@link DataJournalEntry}. Each {@link SegmentedJournalActor} has its own instance, as well as
 * a nested JavaSerializer to handle the payload.
 *
 * <p>
 * Since we are persisting only parts of {@link PersistentRepr}, this class asymmetric by design:
 * {@link #write(EntryOutput, DataJournalEntry)} only accepts {@link ToPersistence} subclass, which is a wrapper
 * around a {@link PersistentRepr}, while {@link #read(EntryInput)} produces an {@link FromPersistence}, which
 * needs further processing to reconstruct a {@link PersistentRepr}.
 *
 * @author Robert Varga
 */
final class DataJournalEntrySerializer implements EntrySerdes<DataJournalEntry>, JournalSerdes {
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private final ExtendedActorSystem actorSystem;

    DataJournalEntrySerializer(final ActorSystem actorSystem) {
        this.actorSystem = requireNonNull((ExtendedActorSystem) actorSystem);
    }

    @Override
    public void write(final EntryOutput output, final Object entry) throws IOException {
        verify(entry instanceof ToPersistence);
        final PersistentRepr repr = ((ToPersistence) entry).repr();
        output.writeString(repr.manifest());
        output.writeString(repr.writerUuid());
        output.writeObject(repr.payload());
    }

    @Override
    public DataJournalEntry read(final EntryInput input) throws IOException {
        final String manifest = input.readString();
        final String uuid = input.readString();
        final Object payload = akka.serialization.JavaSerializer.currentSystem().withValue(actorSystem,
            (Callable<Object>) input::readObject);
        return new FromPersistence(manifest, uuid, payload);
    }

    @Override
    public byte[] serialize(Object obj) {
        var buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        serialize(obj, buffer);
        return buffer.array();
    }

    @Override
    public byte[] serialize(Object obj, int bufferSize) {
        var buffer = ByteBuffer.allocate(bufferSize);
        serialize(obj, buffer);
        return buffer.array();
    }

    @Override
    public void serialize(Object obj, ByteBuffer buffer) {
//        var kryoOut = new MyOutput(DEFAULT_BUFFER_SIZE);
//        kryoOut.writeVarInt(18, true);
//        kryoOut.writeVarInt(1, true);
//        final PersistentRepr repr = ((ToPersistence) obj).repr();
//        kryoOut.writeString(repr.manifest());
//        kryoOut.writeString(repr.writerUuid());
//
//        try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
//            try (ObjectOutputStream o = new ObjectOutputStream(b)) {
//                o.writeObject(repr.payload());
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            buffer.put(kryoOut.toBytes());
//            buffer.put(b.toByteArray());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        // tag new serializer
        buffer.put((byte) 19);
        try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            try (ObjectOutputStream o = new ObjectOutputStream(b)) {
                final PersistentRepr repr = ((ToPersistence) obj).repr();
                o.writeObject(repr.manifest());
                o.writeObject(repr.writerUuid());
                o.writeObject(repr.payload());
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
        serialize(obj, stream, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public void serialize(Object obj, OutputStream stream, int bufferSize) {
        var kryoOut = new MyOutput(DEFAULT_BUFFER_SIZE);
        kryoOut.writeVarInt(18, true);
        kryoOut.writeVarInt(1, true);
        final PersistentRepr repr = ((ToPersistence) obj).repr();
        kryoOut.writeString(repr.manifest());
        kryoOut.writeString(repr.writerUuid());

        try (ObjectOutputStream o = new ObjectOutputStream(stream)) {
            o.write(kryoOut.toBytes());
            o.writeObject(repr.payload());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes) {
        return deserialize(new ByteArrayInputStream(bytes));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(ByteBuffer buffer) {
        if (buffer.get() == 18) {
            return legacyDeserialize(buffer);
        }
        //ONLY this one is used
        try (var byteBufferBackedInputStream = new ByteBufferBackedInputStream(buffer)) {
            try (ObjectInputStream o = new ObjectInputStream(byteBufferBackedInputStream)) {
                final String manifest = (String) o.readObject();
                final String uuid = (String) o.readObject();
                final Object payload = akka.serialization.JavaSerializer.currentSystem().withValue(actorSystem,
                        (Callable<Object>) o::readObject);
                return (T) new FromPersistence(manifest, uuid, payload);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(InputStream stream) {
        //UNUSED
        var legacyInput = new LegacyInput(stream);
        int mark1 = legacyInput.readVarInt(true);
        int mark2 = legacyInput.readVarInt(true);
        if (mark1 != 18) {
            throw new RuntimeException("actual mark1: " + mark1);
        }
        if (mark2 != 1) {
            throw new RuntimeException("actual mark2: " + mark2);
        }
        final String manifest = legacyInput.readString();
        final String uuid = legacyInput.readString();
        try (ObjectInputStream o = new ObjectInputStream(legacyInput)) {
            final Object payload = o.readObject();
            return (T) new FromPersistence(manifest, uuid, payload);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T deserialize(InputStream stream, int bufferSize) {
        return deserialize(stream);
    }

    @SuppressWarnings("unchecked")
    private <T> T legacyDeserialize(ByteBuffer buffer) {
        //ONLY this one is used
        var legacyByteBufferInput = new LegacyByteBufferInput(buffer);
        //int mark1 = legacyByteBufferInput.readVarInt(true);
        int mark2 = legacyByteBufferInput.readVarInt(true);
//        if (mark1 != 18) {
//            throw new RuntimeException("actual mark1: " + mark1);
//        }
        if (mark2 != 1) {
            throw new RuntimeException("actual mark2: " + mark2);
        }
        final String manifest = legacyByteBufferInput.readString();
        final String uuid = legacyByteBufferInput.readString();
        try (ObjectInputStream o = new ObjectInputStream(legacyByteBufferInput)) {
            final Object payload = akka.serialization.JavaSerializer.currentSystem().withValue(actorSystem,
                    (Callable<Object>) o::readObject);
            return (T) new FromPersistence(manifest, uuid, payload);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
