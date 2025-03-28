/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.math.IntMath.ceilingPowerOfTwo;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import org.apache.commons.lang3.SerializationUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.raft.messages.IdentifiablePayload;
import org.opendaylight.raft.spi.ByteArray;
import org.opendaylight.raft.spi.ChunkedOutputStream;
import org.opendaylight.yangtools.yang.data.api.schema.stream.ReusableStreamReceiver;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion;
import org.opendaylight.yangtools.yang.data.impl.schema.ReusableImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payload persisted when a transaction commits. It contains the transaction identifier and the
 * {@link DataTreeCandidate}.
 */
@Beta
public final class CommitTransactionPayload extends IdentifiablePayload<TransactionIdentifier> implements Serializable {
    @NonNullByDefault
    public record CandidateTransaction(
            TransactionIdentifier transactionId,
            DataTreeCandidate candidate,
            NormalizedNodeStreamVersion streamVersion) {
        public CandidateTransaction {
            requireNonNull(transactionId);
            requireNonNull(candidate);
            requireNonNull(streamVersion);
        }
    }

    // Exists to break initialization dependency between CommitTransactionPayload/Simple/Proxy
    private static final class ProxySizeHolder {
        static final int PROXY_SIZE = SerializationUtils.serialize(new CT(ByteArray.empty())).length;

        private ProxySizeHolder() {
            // Hidden on purpose
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(CommitTransactionPayload.class);
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    static final int MAX_ARRAY_SIZE = ceilingPowerOfTwo(Integer.getInteger(
        "org.opendaylight.controller.cluster.datastore.persisted.max-array-size", 256 * 1024));

    private final @NonNull ByteArray source;

    private volatile CandidateTransaction candidate = null;

    CommitTransactionPayload(final ByteArray source) {
        this.source = requireNonNull(source);
    }

    public static @NonNull CommitTransactionPayload create(final TransactionIdentifier transactionId,
            final DataTreeCandidate candidate, final PayloadVersion version, final int initialSerializedBufferCapacity)
                    throws IOException {
        final var cos = new ChunkedOutputStream(initialSerializedBufferCapacity, MAX_ARRAY_SIZE);
        try (var dos = new DataOutputStream(cos)) {
            transactionId.writeTo(dos);
            DataTreeCandidateInputOutput.writeDataTreeCandidate(dos, version, candidate);
        }

        final var source = cos.toByteArray();
        LOG.debug("Initial buffer capacity {}, actual serialized size {}", initialSerializedBufferCapacity,
            source.size());
        return new CommitTransactionPayload(source);
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

    public @NonNull CandidateTransaction getCandidate() throws IOException {
        var localCandidate = candidate;
        if (localCandidate == null) {
            synchronized (this) {
                localCandidate = candidate;
                if (localCandidate == null) {
                    candidate = localCandidate = getCandidate(ReusableImmutableNormalizedNodeStreamWriter.create());
                }
            }
        }
        return localCandidate;
    }

    public @NonNull CandidateTransaction getCandidate(final ReusableStreamReceiver receiver) throws IOException {
        final var in = source.openDataInput();
        final var transactionId = TransactionIdentifier.readFrom(in);
        final var readCandidate = DataTreeCandidateInputOutput.readDataTreeCandidate(in, receiver);

        return new CandidateTransaction(transactionId, readCandidate.candidate(), readCandidate.version());
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        try  {
            return getCandidate().transactionId();
        } catch (IOException e) {
            throw new IllegalStateException("Candidate deserialization failed.", e);
        }
    }

    @Override
    public int serializedSize() {
        // TODO: this is not entirely accurate as the the byte[] can be chunked by the serialization stream
        return ProxySizeHolder.PROXY_SIZE + size();
    }

    @Override
    public int size() {
        return source.size();
    }

    /**
     * The cached candidate needs to be cleared after it is done applying to the DataTree, otherwise it would be keeping
     * deserialized in memory which are not needed anymore leading to wasted memory. This lets the payload know that
     * this was the last time the candidate was needed ant it is safe to be cleared.
     */
    public @NonNull CandidateTransaction acquireCandidate() throws IOException {
        final var localCandidate = getCandidate();
        candidate = null;
        return localCandidate;
    }

    @Override
    public String toString() {
        final var helper = MoreObjects.toStringHelper(this);
        final var localCandidate = candidate;
        if (localCandidate != null) {
            helper.add("identifier", candidate.transactionId());
        }
        return helper.add("size", size()).toString();
    }

    @Override
    public Object writeReplace() {
        return new CT(source);
    }

    @java.io.Serial
    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        throwNSE();
    }

    @java.io.Serial
    private void readObjectNoData() throws ObjectStreamException {
        throwNSE();
    }

    @java.io.Serial
    private void writeObject(final ObjectOutputStream stream) throws IOException {
        throwNSE();
    }

    private static void throwNSE() throws NotSerializableException {
        throw new NotSerializableException(CommitTransactionPayload.class.getName());
    }
}
