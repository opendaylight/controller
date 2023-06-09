/*
 * Copyright 2014-2021 Open Networking Foundation
 * Copyright 2023 PANTHEON.tech, s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.storage.journal;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import io.atomix.utils.serializer.JournalSerdesImplBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Support for serialization of {@link Journal} entries.
 */
public interface JournalSerdes {
    /**
     * Serializes given object to byte array.
     *
     * @param obj Object to serialize
     * @return serialized bytes
     */
    byte[] serialize(Object obj);

    /**
     * Serializes given object to byte array.
     *
     * @param obj        Object to serialize
     * @param bufferSize maximum size of serialized bytes
     * @return serialized bytes
     */
    byte[] serialize(Object obj, int bufferSize);

    /**
     * Serializes given object to byte buffer.
     *
     * @param obj    Object to serialize
     * @param buffer to write to
     */
    void serialize(Object obj, ByteBuffer buffer);

    /**
     * Serializes given object to OutputStream.
     *
     * @param obj    Object to serialize
     * @param stream to write to
     */
    void serialize(Object obj, OutputStream stream);

    /**
     * Serializes given object to OutputStream.
     *
     * @param obj        Object to serialize
     * @param stream     to write to
     * @param bufferSize size of the buffer in front of the stream
     */
    void serialize(Object obj, OutputStream stream, int bufferSize);

    /**
     * Deserializes given byte array to Object.
     *
     * @param bytes serialized bytes
     * @param <T>   deserialized Object type
     * @return deserialized Object
     */
    <T> T deserialize(byte[] bytes);

    /**
     * Deserializes given byte buffer to Object.
     *
     * @param buffer input with serialized bytes
     * @param <T>    deserialized Object type
     * @return deserialized Object
     */
    <T> T deserialize(final ByteBuffer buffer);

    /**
     * Deserializes given InputStream to an Object.
     *
     * @param stream input stream
     * @param <T>    deserialized Object type
     * @return deserialized Object
     */
    <T> T deserialize(InputStream stream);

    /**
     * Deserializes given InputStream to an Object.
     *
     * @param stream     input stream
     * @param <T>        deserialized Object type
     * @param bufferSize size of the buffer in front of the stream
     * @return deserialized Object
     */
    <T> T deserialize(final InputStream stream, final int bufferSize);

    /**
     * Creates a new {@link JournalSerdes} builder.
     *
     * @return builder
     */
    static Builder builder() {
        return new JournalSerdesImplBuilder();
    }

    /**
     * Builder for {@link JournalSerdes}.
     */
    interface Builder {
        /**
         * Builds a {@link JournalSerdes} instance.
         *
         * @return A {@link JournalSerdes} implementation.
         */
        JournalSerdes build();

        /**
         * Builds a {@link JournalSerdes} instance.
         *
         * @param friendlyName friendly name for the namespace
         * @return A {@link JournalSerdes} implementation.
         */
        JournalSerdes build(String friendlyName);

        /**
         * Registers serializer for the given set of classes.
         * <p>
         * When multiple classes are registered with an explicitly provided serializer, the namespace guarantees
         * all instances will be serialized with the same type ID.
         *
         * @param classes list of classes to register
         * @param serdes  serializer to use for the class
         * @return this builder
         */
        Builder register(EntrySerdes<?> serdes, Class<?>... classes);

        /**
         * Sets the namespace class loader.
         *
         * @param classLoader the namespace class loader
         * @return this builder
         */
        Builder setClassLoader(ClassLoader classLoader);
    }

    /**
     * Input data stream exposed to {@link EntrySerdes#read(EntryInput)}.
     */
    @Beta
    interface EntryInput {

        byte[] readBytes(int length) throws IOException;

        long readLong() throws IOException;

        String readString() throws IOException;

        Object readObject() throws IOException;

        @VisibleForTesting
        int readVarInt() throws IOException;
    }

    /**
     * Output data stream exposed to {@link EntrySerdes#write(EntryOutput, Object)}.
     */
    @Beta
    interface EntryOutput {

        void writeBytes(byte[] bytes) throws IOException;

        void writeLong(long value) throws IOException;

        void writeObject(Object value) throws IOException;

        void writeString(String value) throws IOException;

        @VisibleForTesting
        void writeVarInt(int value) throws IOException;
    }

    /**
     * A serializer/deserializer for an entry.
     *
     * @param <T> Entry type
     */
    interface EntrySerdes<T> {

        T read(EntryInput input) throws IOException;

        void write(EntryOutput output, T entry) throws IOException;
    }
}
