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
import com.google.common.base.Throwables;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.DataInput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.Optional;
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
public final class CommitTransactionPayload extends Payload implements DataTreeCandidateSupplier, Identifiable<TransactionIdentifier>, Serializable {
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
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(serialized.length);
            out.write(serialized);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            final int length = in.readInt();
            serialized = new byte[length];
            in.readFully(serialized);
        }

        private Object readResolve() {
            return new CommitTransactionPayload(serialized, null);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(CommitTransactionPayload.class);
    private static final long serialVersionUID = 1L;

    private final byte[] serialized;

    private volatile TransactionIdentifier identifier;

    CommitTransactionPayload(final byte[] serialized, final TransactionIdentifier identifier) {
        this.serialized = Preconditions.checkNotNull(serialized);
        this.identifier = identifier;
    }

    public static CommitTransactionPayload create(final TransactionIdentifier transactionId,
            final DataTreeCandidate candidate) throws IOException {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        transactionId.writeTo(out);
        DataTreeCandidateInputOutput.writeDataTreeCandidate(out, candidate);
        return new CommitTransactionPayload(out.toByteArray(), transactionId);
    }

    @Override
    public Entry<Optional<TransactionIdentifier>, DataTreeCandidate> getCandidate() throws IOException {
        final DataInput in = ByteStreams.newDataInput(serialized);
        return new SimpleImmutableEntry<>(Optional.of(TransactionIdentifier.readFrom(in)),
                DataTreeCandidateInputOutput.readDataTreeCandidate(in));
    }

    @Override
    public int size() {
        return serialized.length;
    }

    private Object writeReplace() {
        return new Proxy(serialized);
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        TransactionIdentifier local = identifier;
        if (local == null) {
            synchronized (this) {
                local = identifier;
                if (local == null) {
                    LOG.warn("Inefficient code path: getCandidate() should have been called");
                    try {
                        local = getCandidate().getKey();
                    } catch (IOException e) {
                        throw Throwables.propagate(e);
                    }
                    identifier = local;
                }
            }
        }

        return local;
    }
}
