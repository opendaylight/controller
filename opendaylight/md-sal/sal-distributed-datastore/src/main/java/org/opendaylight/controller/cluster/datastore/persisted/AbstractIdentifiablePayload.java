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
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.function.Function;
import org.apache.commons.lang3.SerializationUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.messages.IdentifiablePayload;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Abstract base class for {@link IdentifiablePayload}s which hold a single {@link Identifier}.
 *
 * @author Robert Varga
 */
public abstract class AbstractIdentifiablePayload<T extends Identifier> extends IdentifiablePayload<T>
        implements Serializable {
    protected abstract static class AbstractProxy<T extends Identifier> implements Externalizable {
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
        public final void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(serialized.length);
            out.write(serialized);
        }

        @Override
        public final void readExternal(final ObjectInput in) throws IOException {
            final int length = in.readInt();
            serialized = new byte[length];
            in.readFully(serialized);
            identifier = verifyNotNull(readIdentifier(ByteStreams.newDataInput(serialized)));
        }

        protected final Object readResolve() {
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
    protected final Object writeReplace() {
        return verifyNotNull(externalizableProxy(serialized));
    }

    @SuppressWarnings("checkstyle:hiddenField")
    protected abstract @NonNull AbstractProxy<T> externalizableProxy(byte @NonNull[] serialized);

    protected abstract int externalizableProxySize();

    protected static final int externalizableProxySize(final Function<byte[], ? extends AbstractProxy<?>> constructor) {
        return SerializationUtils.serialize(constructor.apply(new byte[0])).length;
    }
}
