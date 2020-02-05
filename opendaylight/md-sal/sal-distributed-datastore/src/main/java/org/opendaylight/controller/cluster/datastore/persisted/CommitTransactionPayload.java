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
import static org.opendaylight.controller.cluster.datastore.persisted.ChunkedOutputStream.MAX_ARRAY_SIZE;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.Map.Entry;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.DataTreeCandidateInputOutput.DataTreeCandidateWithVersion;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.yangtools.concepts.Variant;
import org.opendaylight.yangtools.yang.data.api.schema.stream.ReusableStreamReceiver;
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

    CommitTransactionPayload() {

    }

    public static @NonNull CommitTransactionPayload create(final TransactionIdentifier transactionId,
            final DataTreeCandidate candidate, final PayloadVersion version, final int initialSerializedBufferCapacity)
                    throws IOException {
        final ChunkedOutputStream cos = new ChunkedOutputStream(initialSerializedBufferCapacity);
        try (DataOutputStream dos = new DataOutputStream(cos)) {
            transactionId.writeTo(dos);
            DataTreeCandidateInputOutput.writeDataTreeCandidate(dos, version, candidate);
        }

        final Variant<byte[], ChunkedByteArray> source = cos.toVariant();
        LOG.debug("Initial buffer capacity {}, actual serialized size {}", initialSerializedBufferCapacity, cos.size());
        return source.isFirst() ? new Simple(source.getFirst()) : new Chunked(source.getSecond());
    }

    @VisibleForTesting
    public static @NonNull CommitTransactionPayload create(final TransactionIdentifier transactionId,
            final DataTreeCandidate candidate, final PayloadVersion version) throws IOException {
        return create(transactionId, candidate, version, 512);
    }

    @VisibleForTesting
    public static @NonNull CommitTransactionPayload create(final TransactionIdentifier transactionId,
            final DataTreeCandidate candidate) throws IOException {
        return create(transactionId, candidate, PayloadVersion.current());
    }

    public @NonNull Entry<TransactionIdentifier, DataTreeCandidateWithVersion> getCandidate() throws IOException {
        return getCandidate(ReusableImmutableNormalizedNodeStreamWriter.create());
    }

    public final @NonNull Entry<TransactionIdentifier, DataTreeCandidateWithVersion> getCandidate(
            final ReusableStreamReceiver receiver) throws IOException {
        final DataInput in = newDataInput();
        return new SimpleImmutableEntry<>(TransactionIdentifier.readFrom(in),
                DataTreeCandidateInputOutput.readDataTreeCandidate(in, receiver));
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

        @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "Handled via serialization proxy")
        private final ChunkedByteArray source;

        Chunked(final ChunkedByteArray source) {
            this.source = requireNonNull(source);
        }

        @Override
        void writeBytes(final ObjectOutput out) throws IOException {
            source.copyTo(out);
        }

        @Override
        public int size() {
            return source.size();
        }

        @Override
        DataInput newDataInput() {
            return new DataInputStream(source.openStream());
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
            } else if (length < MAX_ARRAY_SIZE) {
                final byte[] serialized = new byte[length];
                in.readFully(serialized);
                payload = new Simple(serialized);
            } else {
                payload = new Chunked(ChunkedByteArray.readFrom(in, length, MAX_ARRAY_SIZE));
            }
        }

        private Object readResolve() {
            return verifyNotNull(payload);
        }
    }
}
