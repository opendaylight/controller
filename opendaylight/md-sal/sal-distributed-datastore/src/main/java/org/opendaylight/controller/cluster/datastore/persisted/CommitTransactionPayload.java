/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.DataInput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payload persisted when a transaction commits. It contains the transaction identifier and the
 * {@link DataTreeCandidate}
 *
 * @author Robert Varga
 */
@Beta
public final class CommitTransactionPayload extends Payload
        implements Identifiable<TransactionIdentifier>, Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(CommitTransactionPayload.class);

    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private byte[] serialized;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final byte[] serialized) {
            this.serialized = Preconditions.checkNotNull(serialized);
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(serialized.length);
            out.write(serialized);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException {
            final int length = in.readInt();
            serialized = new byte[length];
            in.readFully(serialized);
        }

        private Object readResolve() {
            return new CommitTransactionPayload(serialized);
        }
    }

    private static final long serialVersionUID = 1L;

    private final byte[] serialized;

    private TransactionIdentifier identifier = null;
    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not serializable but this class is.")
    private DataTreeCandidate candidate = null;

    CommitTransactionPayload(final byte[] serialized) {
        this.serialized = Preconditions.checkNotNull(serialized);
    }

    public static CommitTransactionPayload create(final TransactionIdentifier transactionId,
            final DataTreeCandidate candidate, final int initialSerializedBufferCapacity) throws IOException {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput(initialSerializedBufferCapacity);
        transactionId.writeTo(out);
        DataTreeCandidateInputOutput.writeDataTreeCandidate(out, candidate);
        final byte[] serialized = out.toByteArray();

        LOG.debug("Initial buffer capacity {}, actual serialized size {}",
                initialSerializedBufferCapacity, serialized.length);

        return new CommitTransactionPayload(serialized);
    }

    @VisibleForTesting
    public static CommitTransactionPayload create(final TransactionIdentifier transactionId,
                                                  final DataTreeCandidate candidate) throws IOException {
        return create(transactionId, candidate, 512);
    }

    public Entry<TransactionIdentifier, DataTreeCandidate> getCandidate() throws IOException {
        if (candidate == null) {
            deserialize();
        }
        return new SimpleImmutableEntry<>(identifier, candidate);
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        if (identifier == null) {
            try {
                deserialize();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return identifier;
    }

    private void deserialize() throws IOException {
        final DataInput in = ByteStreams.newDataInput(serialized);
        identifier = TransactionIdentifier.readFrom(in);
        candidate = DataTreeCandidateInputOutput.readDataTreeCandidate(in);
    }

    @Override
    public int size() {
        return serialized.length;
    }

    private Object writeReplace() {
        return new Proxy(serialized);
    }
}
