/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * Payload persisted when a transaction is aborted. It contains the transaction identifier.
 *
 * @author Robert Varga
 */
public final class AbortTransactionPayload implements Identifiable<TransactionIdentifier>, Payload {
    private static final class Proxy implements Externalizable {
        private byte[] serialized;
        private TransactionIdentifier transactionId;

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
            transactionId = TransactionIdentifier.readFrom(ByteStreams.newDataInput(serialized));
        }

        private Object readResolve() {
            return new AbortTransactionPayload(transactionId, serialized);
        }
    }

    private static final long serialVersionUID = 1L;
    private final TransactionIdentifier transactionId;
    private final byte[] serialized;

    AbortTransactionPayload(final TransactionIdentifier transactionId, final byte[] serialized) {
        this.serialized = Preconditions.checkNotNull(serialized);
        this.transactionId = Preconditions.checkNotNull(transactionId);
    }

    public static AbortTransactionPayload create(final TransactionIdentifier transactionId) throws IOException {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        transactionId.writeTo(out);
        return new AbortTransactionPayload(transactionId, out.toByteArray());
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return transactionId;
    }

    @Override
    public int size() {
        return serialized.length;
    }

    private Object writeReplace() {
        return new Proxy(serialized);
    }
}
