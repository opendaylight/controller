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

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.impl.schema.ReusableImmutableNormalizedNodeStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payload persisted when a transaction commits. It contains the transaction identifier and the
 * {@link DataTreeCandidate}
 *
 * @author Robert Varga
 */
@Beta
public abstract class CommitTransactionPayload extends Payload implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(CommitTransactionPayload.class);
    private static final long serialVersionUID = 1L;

    private static final int MAX_BYTEARRAY_SIZE = Integer.getInteger(
        "org.opendaylight.controller.cluster.datastore.persisted.max-bytearray-size", 1048576) - 32;

    CommitTransactionPayload() {

    }

    public static CommitTransactionPayload create(final TransactionIdentifier transactionId,
            final DataTreeCandidate candidate, final int initialSerializedBufferCapacity) throws IOException {

        final ChunkedOutputStream cos = new ChunkedOutputStream(initialSerializedBufferCapacity);
        try (DataOutputStream dos = new DataOutputStream(cos)) {
            transactionId.writeTo(dos);
            DataTreeCandidateInputOutput.writeDataTreeCandidate(dos, candidate);
        }

        final List<byte[]> chunks = cos.getChunks();
        final int size = cos.getSize();
        LOG.debug("Initial buffer capacity {}, actual serialized size {}", initialSerializedBufferCapacity, size);

        return chunks.size() == 1 ? new Simple(chunks.get(0)) : new Chunked(size, chunks);
    }

    @VisibleForTesting
    public static CommitTransactionPayload create(final TransactionIdentifier transactionId,
            final DataTreeCandidate candidate) throws IOException {
        return create(transactionId, candidate, 512);
    }

    public Entry<TransactionIdentifier, DataTreeCandidate> getCandidate() throws IOException {
        return getCandidate(ReusableImmutableNormalizedNodeStreamWriter.create());
    }

    public final Entry<TransactionIdentifier, DataTreeCandidate> getCandidate(
            final ReusableImmutableNormalizedNodeStreamWriter writer) throws IOException {
        final DataInput in = newDataInput();
        return new SimpleImmutableEntry<>(TransactionIdentifier.readFrom(in),
                DataTreeCandidateInputOutput.readDataTreeCandidate(in, writer));
    }

    abstract void writeBytes(ObjectOutput out) throws IOException;

    abstract DataInput newDataInput();

    final Object writeReplace() {
        return new Proxy(this);
    }

    private static final class Simple extends CommitTransactionPayload {
        private static final long serialVersionUID = 1L;

        private final byte[] serialized;

        Simple(final byte[] serialized) {
            this.serialized = requireNonNull(serialized);
        }

        static Simple readFrom(final ObjectInput in, final int length) throws IOException {
            final byte[] serialized = new byte[length];
            in.readFully(serialized);
            return new Simple(serialized);
        }

        @Override
        public int size() {
            return serialized.length;
        }

        @Override
        DataInput newDataInput() {
            return ByteStreams.newDataInput(serialized);
        }

        @Override
        void writeBytes(final ObjectOutput out) throws IOException {
            out.write(serialized);
        }
    }

    private static final class Chunked extends CommitTransactionPayload {
        private static final long serialVersionUID = 1L;

        private final ImmutableList<byte[]> chunks;
        private final int size;

        private Chunked(final int size, final List<byte[]> chunks) {
            this.size = size;
            this.chunks = ImmutableList.copyOf(chunks);
        }

        static Chunked readFrom(final ObjectInput in, final int length) throws IOException {
            final List<byte[]> chunks = new ArrayList<>();

            int remaining = length;
            do {
                final byte[] buffer = new byte[remaining > MAX_BYTEARRAY_SIZE ? MAX_BYTEARRAY_SIZE : remaining];
                in.readFully(buffer);
                chunks.add(buffer);
                remaining -= buffer.length;
            } while (remaining != 0);

            return new Chunked(length, chunks);
        }

        @Override
        void writeBytes(final ObjectOutput out) throws IOException {
            for (byte[] chunk : chunks) {
                out.write(chunk);
            }
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        DataInput newDataInput() {
            return new DataInputStream(new ChunkedInputStream(size, chunks));
        }
    }

    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private CommitTransactionPayload payload;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final CommitTransactionPayload payload) {
            this.payload = requireNonNull(payload);
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(payload.size());
            payload.writeBytes(out);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException {
            final int length = in.readInt();
            if (length < 0) {
                throw new StreamCorruptedException("Invalid payload length " + length);
            } else if (length < MAX_BYTEARRAY_SIZE) {
                payload = Simple.readFrom(in, length);
            } else {
                payload = Chunked.readFrom(in, length);
            }
        }

        private Object readResolve() {
            return verifyNotNull(payload);
        }
    }
}
