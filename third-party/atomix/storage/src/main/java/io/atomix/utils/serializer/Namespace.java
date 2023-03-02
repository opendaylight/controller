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
package io.atomix.utils.serializer;

import static java.util.Objects.requireNonNull;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import io.atomix.storage.journal.JournalSerdes;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pool of Kryo instances, with classes pre-registered.
 */
public final class Namespace implements JournalSerdes, KryoFactory, KryoPool {
    /**
     * Default buffer size used for serialization.
     *
     * @see #serialize(Object)
     */
    private static final int DEFAULT_BUFFER_SIZE = 4096;

    /**
     * ID to use if this KryoNamespace does not define registration id.
     */
    private static final int FLOATING_ID = -1;

    /**
     * Smallest ID free to use for user defined registrations.
     */
    private static final int INITIAL_ID = 16;

    private static final String NO_NAME = "(no name)";

    private static final Logger LOGGER = LoggerFactory.getLogger(Namespace.class);

    private final KryoPool kryoPool = new KryoPool.Builder(this).softReferences().build();

    private final KryoOutputPool kryoOutputPool = new KryoOutputPool();
    private final KryoInputPool kryoInputPool = new KryoInputPool();

    private final ImmutableList<RegistrationBlock> registeredBlocks;

    private final ClassLoader classLoader;
    private final String friendlyName;

    /**
     * KryoNamespace builder.
     */
    private static final class Builder implements JournalSerdes.Builder {
        private final int blockHeadId = INITIAL_ID;
        private final List<Entry<Class<?>[], EntrySerializer<?>>> types = new ArrayList<>();
        private final List<RegistrationBlock> blocks = new ArrayList<>();
        private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        @Override
        public Builder register(final EntrySerdes<?> serdes, final Class<?>... classes) {
            types.add(Map.entry(classes, new EntrySerializer<>(serdes)));
            return this;
        }

        @Override
        public Builder setClassLoader(final ClassLoader classLoader) {
            this.classLoader = requireNonNull(classLoader);
            return this;
        }

        @Override
        public JournalSerdes build() {
            return build(NO_NAME);
        }

        @Override
        public JournalSerdes build(final String friendlyName) {
            if (!types.isEmpty()) {
                blocks.add(new RegistrationBlock(blockHeadId, types));
            }
            return new Namespace(blocks, classLoader, friendlyName);
        }
    }

    /**
     * Creates a new {@link Namespace} builder.
     *
     * @return builder
     */
    public static JournalSerdes.Builder builder() {
        return new Builder();
    }

    /**
     * Creates a Kryo instance pool.
     *
     * @param registeredTypes      types to register
     * @param registrationRequired whether registration is required
     * @param friendlyName         friendly name for the namespace
     */
    private Namespace(
        final List<RegistrationBlock> registeredTypes,
        final ClassLoader classLoader,
        final String friendlyName) {
        registeredBlocks = ImmutableList.copyOf(registeredTypes);
        this.classLoader = classLoader;
        this.friendlyName = requireNonNull(friendlyName);

        // Pre-populate with a single instance
        release(create());
    }

    @Override
    public byte[] serialize(final Object obj) {
        return serialize(obj, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public byte[] serialize(final Object obj, final int bufferSize) {
        return kryoOutputPool.run(output -> kryoPool.run(kryo -> {
            kryo.writeClassAndObject(output, obj);
            output.flush();
            return output.getByteArrayOutputStream().toByteArray();
        }), bufferSize);
    }

    @Override
    public void serialize(final Object obj, final ByteBuffer buffer) {
        ByteBufferOutput out = new ByteBufferOutput(buffer);
        Kryo kryo = borrow();
        try {
            kryo.writeClassAndObject(out, obj);
            out.flush();
        } finally {
            release(kryo);
        }
    }

    @Override
    public void serialize(final Object obj, final OutputStream stream) {
        serialize(obj, stream, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public void serialize(final Object obj, final OutputStream stream, final int bufferSize) {
        ByteBufferOutput out = new ByteBufferOutput(stream, bufferSize);
        Kryo kryo = borrow();
        try {
            kryo.writeClassAndObject(out, obj);
            out.flush();
        } finally {
            release(kryo);
        }
    }

    @Override
    public <T> T deserialize(final byte[] bytes) {
        return kryoInputPool.run(input -> {
            input.setInputStream(new ByteArrayInputStream(bytes));
            return kryoPool.run(kryo -> {
                @SuppressWarnings("unchecked")
                T obj = (T) kryo.readClassAndObject(input);
                return obj;
            });
        }, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public <T> T deserialize(final ByteBuffer buffer) {
        ByteBufferInput in = new ByteBufferInput(buffer);
        Kryo kryo = borrow();
        try {
            @SuppressWarnings("unchecked")
            T obj = (T) kryo.readClassAndObject(in);
            return obj;
        } finally {
            release(kryo);
        }
    }

    @Override
    public <T> T deserialize(final InputStream stream) {
        return deserialize(stream, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public <T> T deserialize(final InputStream stream, final int bufferSize) {
        ByteBufferInput in = new ByteBufferInput(stream, bufferSize);
        Kryo kryo = borrow();
        try {
            @SuppressWarnings("unchecked")
            T obj = (T) kryo.readClassAndObject(in);
            return obj;
        } finally {
            release(kryo);
        }
    }

    /**
     * Creates a Kryo instance.
     *
     * @return Kryo instance
     */
    @Override
    public Kryo create() {
        LOGGER.trace("Creating Kryo instance for {}", this);
        Kryo kryo = new Kryo();
        kryo.setClassLoader(classLoader);
        kryo.setRegistrationRequired(true);

        // TODO rethink whether we want to use StdInstantiatorStrategy
        kryo.setInstantiatorStrategy(
            new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

        for (RegistrationBlock block : registeredBlocks) {
            int id = block.begin();
            if (id == FLOATING_ID) {
                id = kryo.getNextRegistrationId();
            }
            for (Entry<Class<?>[], EntrySerializer<?>> entry : block.types()) {
                register(kryo, entry.getKey(), entry.getValue(), id++);
            }
        }
        return kryo;
    }

    /**
     * Register {@code type} and {@code serializer} to {@code kryo} instance.
     *
     * @param kryo       Kryo instance
     * @param types      types to register
     * @param serializer Specific serializer to register or null to use default.
     * @param id         type registration id to use
     */
    private void register(final Kryo kryo, final Class<?>[] types, final Serializer<?> serializer, final int id) {
        Registration existing = kryo.getRegistration(id);
        if (existing != null) {
            boolean matches = false;
            for (Class<?> type : types) {
                if (existing.getType() == type) {
                    matches = true;
                    break;
                }
            }

            if (!matches) {
                LOGGER.error("{}: Failed to register {} as {}, {} was already registered.",
                    friendlyName, types, id, existing.getType());

                throw new IllegalStateException(String.format(
                    "Failed to register %s as %s, %s was already registered.",
                    Arrays.toString(types), id, existing.getType()));
            }
            // falling through to register call for now.
            // Consider skipping, if there's reasonable
            // way to compare serializer equivalence.
        }

        for (Class<?> type : types) {
            Registration r = null;
            if (serializer == null) {
                r = kryo.register(type, id);
            } else if (type.isInterface()) {
                kryo.addDefaultSerializer(type, serializer);
            } else {
                r = kryo.register(type, serializer, id);
            }
            if (r != null) {
                if (r.getId() != id) {
                    LOGGER.debug("{}: {} already registered as {}. Skipping {}.",
                        friendlyName, r.getType(), r.getId(), id);
                }
                LOGGER.trace("{} registered as {}", r.getType(), r.getId());
            }
        }
    }

    @Override
    public Kryo borrow() {
        return kryoPool.borrow();
    }

    @Override
    public void release(final Kryo kryo) {
        kryoPool.release(kryo);
    }

    @Override
    public <T> T run(final KryoCallback<T> callback) {
        return kryoPool.run(callback);
    }

    @Override
    public String toString() {
        if (!NO_NAME.equals(friendlyName)) {
            return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("friendlyName", friendlyName)
                // omit lengthy detail, when there's a name
                .toString();
        }
        return MoreObjects.toStringHelper(getClass()).add("registeredBlocks", registeredBlocks).toString();
    }

    static final class RegistrationBlock {
        private final int begin;
        private final ImmutableList<Entry<Class<?>[], EntrySerializer<?>>> types;

        RegistrationBlock(final int begin, final List<Entry<Class<?>[], EntrySerializer<?>>> types) {
            this.begin = begin;
            this.types = ImmutableList.copyOf(types);
        }

        public int begin() {
            return begin;
        }

        public ImmutableList<Entry<Class<?>[], EntrySerializer<?>>> types() {
            return types;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass()).add("begin", begin).add("types", types).toString();
        }

        @Override
        public int hashCode() {
            return types.hashCode();
        }

        // Only the registered types are used for equality.
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof RegistrationBlock that) {
                return Objects.equals(types, that.types);
            }
            return false;
        }
    }
}
