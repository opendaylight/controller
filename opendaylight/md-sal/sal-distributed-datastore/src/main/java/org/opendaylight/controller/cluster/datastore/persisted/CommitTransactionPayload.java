/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.DataInput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * Payload persisted when a transaction commits. It contains the transaction identifier and the
 * {@link DataTreeCandidate}
 *
 * @author Robert Varga
 */
@Beta
public final class CommitTransactionPayload extends Payload implements DataTreeCandidateSupplier,
        Identifiable<TransactionIdentifier>, Serializable {
    private static final class Decoded implements Identifiable<TransactionIdentifier> {
        private final TransactionIdentifier identifier;
        private final DataTreeCandidate candidate;

        Decoded(TransactionIdentifier identifier, DataTreeCandidate candidate) {
            this.identifier = Preconditions.checkNotNull(identifier);
            this.candidate = Preconditions.checkNotNull(candidate);
        }

        static Decoded fromSerialized(final byte[] serialized) throws IOException {
            final DataInput in = ByteStreams.newDataInput(serialized);
            final TransactionIdentifier identifier = TransactionIdentifier.readFrom(in);
            return new Decoded(identifier, DataTreeCandidateInputOutput.readDataTreeCandidate(in));
        }

        @Override
        public TransactionIdentifier getIdentifier() {
            return identifier;
        }

        public DataTreeCandidate getCandidate() {
            return candidate;
        }
    }


    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private byte[] serialized;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final byte[] serialized) {
            this.serialized = Preconditions.checkNotNull(serialized);
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(serialized.length);
            out.write(serialized);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
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
    private Decoded decoded;

    CommitTransactionPayload(final byte[] serialized) {
        this.serialized = Preconditions.checkNotNull(serialized);
    }

    private CommitTransactionPayload(final byte[] serialized, final TransactionIdentifier transactionId,
            final DataTreeCandidate candidate) {
        this(serialized);
        this.decoded = new Decoded(transactionId, candidate);
    }

    public static CommitTransactionPayload create(final TransactionIdentifier transactionId,
            final DataTreeCandidate candidate) throws IOException {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        transactionId.writeTo(out);
        DataTreeCandidateInputOutput.writeDataTreeCandidate(out, candidate);
        return new CommitTransactionPayload(out.toByteArray(), transactionId, candidate);
    }

    private Decoded getDecoded() throws IOException {
        if (decoded == null) {
            decoded = Decoded.fromSerialized(serialized);
        }
        return decoded;
    }

    @Override
    public DataTreeCandidate getCandidate() throws IOException {
        return getDecoded().getCandidate();
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        try {
            return getDecoded().getIdentifier();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to decode payload", e);
        }
    }

    @Override
    public int size() {
        return serialized.length;
    }

    private Object writeReplace() {
        return new Proxy(serialized);
    }
}
