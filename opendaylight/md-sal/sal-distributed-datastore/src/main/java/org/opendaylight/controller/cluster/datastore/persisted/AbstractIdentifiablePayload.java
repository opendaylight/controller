/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.io.ByteStreams;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.function.Function;
import org.apache.commons.lang3.SerializationUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.messages.IdentifiablePayload;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Abstract base class for {@link IdentifiablePayload}s which hold a single {@link Identifier}.
 */
public abstract class AbstractIdentifiablePayload<T extends Identifier> extends IdentifiablePayload<T> {
    /**
     * An {@link Externalizable} with default implementations we expect our implementations to comply with. On-wire
     * serialization format is defined by {@link #bytes()}.
     */
    protected interface SerialForm extends Externalizable {
        /**
         * Return the serial form of this object contents, corresponding to
         * {@link AbstractIdentifiablePayload#serialized}.
         *
         * @return Serialized form
         */
        byte[] bytes();

        /**
         * Resolve this proxy to an actual {@link AbstractIdentifiablePayload}.
         *
         * @return A payload.
         */
        @java.io.Serial
        Object readResolve();

        /**
         * Restore state from specified serialized form.
         *
         * @param newBytes Serialized form, as returned by {@link #bytes()}
         * @throws IOException when a deserialization problem occurs
         */
        void readExternal(byte[] newBytes) throws IOException;

        /**
         * {@inheritDoc}
         *
         * <p>
         * The default implementation is canonical and should never be overridden.
         */
        @Override
        default void readExternal(final ObjectInput in) throws IOException {
            final var bytes = new byte[in.readInt()];
            in.readFully(bytes);
            readExternal(bytes);
        }

        /**
         * {@inheritDoc}
         *
         * <p>
         * The default implementation is canonical and should never be overridden.
         */
        @Override
        default void writeExternal(final ObjectOutput out) throws IOException {
            final var bytes = bytes();
            out.writeInt(bytes.length);
            out.write(bytes);
        }
    }

    @Deprecated(since = "7.0.0", forRemoval = true)
    protected abstract static class AbstractProxy<T extends Identifier> implements SerialForm {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private byte[] serialized;
        private T identifier;

        public AbstractProxy() {
            // For Externalizable
        }

        protected AbstractProxy(final byte[] serialized) {
            this.serialized = requireNonNull(serialized);
        }

        @Override
        public final byte[] bytes() {
            return serialized;
        }

        @Override
        public final void readExternal(final byte[] bytes) throws IOException {
            serialized = requireNonNull(bytes);
            identifier = verifyNotNull(readIdentifier(ByteStreams.newDataInput(serialized)));
        }

        @Override
        public final Object readResolve() {
            return verifyNotNull(createObject(identifier, serialized));
        }

        protected abstract @NonNull T readIdentifier(@NonNull DataInput in) throws IOException;

        @SuppressWarnings("checkstyle:hiddenField")
        protected abstract @NonNull Identifiable<T> createObject(@NonNull T identifier, byte @NonNull[] serialized);
    }

    private static final long serialVersionUID = 1L;

    private final byte @NonNull [] serialized;
    private final @NonNull T identifier;

    AbstractIdentifiablePayload(final @NonNull T identifier, final byte @NonNull[] serialized) {
        this.identifier = requireNonNull(identifier);
        this.serialized = requireNonNull(serialized);
    }

    @Override
    public final T getIdentifier() {
        return identifier;
    }

    @Override
    public final int size() {
        return serialized.length;
    }

    protected final byte @NonNull [] serialized() {
        return serialized;
    }

    @Override
    public final int serializedSize() {
        // TODO: this is not entirely accurate, as the serialization stream has additional overheads:
        //       - 3 bytes for each block of data <256 bytes
        //       - 5 bytes for each block of data >=256 bytes
        //       - each block of data is limited to 1024 bytes as per serialization spec
        return size() + externalizableProxySize();
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("identifier", identifier).add("size", size()).toString();
    }

    @Override
    public final Object writeReplace() {
        return verifyNotNull(externalizableProxy(serialized));
    }

    protected abstract @NonNull SerialForm externalizableProxy(byte @NonNull[] serialized);

    protected abstract int externalizableProxySize();

    protected static final int externalizableProxySize(final Function<byte[], ? extends SerialForm> constructor) {
        return SerializationUtils.serialize(constructor.apply(new byte[0])).length;
    }

    @Override
    public void writeTo(DataOutput out) throws IOException {
        out.write(serialized());
    }
}
