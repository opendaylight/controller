/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.DataInput;
import java.io.IOException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Payload persisted when a transaction is aborted. It contains the transaction identifier.
 *
 * @author Robert Varga
 */
public final class AbortTransactionPayload extends AbstractIdentifiablePayload<TransactionIdentifier> {
    private static final class Proxy extends AbstractProxy<TransactionIdentifier> {
        private static final long serialVersionUID = 1L;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final byte[] serialized) {
            super(serialized);
        }

        @Override
        protected TransactionIdentifier readIdentifier(final DataInput in) throws IOException {
            return TransactionIdentifier.readFrom(in);
        }

        @Override
        protected AbortTransactionPayload createObject(final TransactionIdentifier identifier,
                final byte[] serialized) {
            return new AbortTransactionPayload(identifier, serialized);
        }
    }

    private static final long serialVersionUID = 1L;

    AbortTransactionPayload(final TransactionIdentifier transactionId, final byte[] serialized) {
        super(transactionId, serialized);
    }

    public static AbortTransactionPayload create(final TransactionIdentifier transactionId) throws IOException {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        transactionId.writeTo(out);
        return new AbortTransactionPayload(transactionId, out.toByteArray());
    }

    @Override
    protected Proxy externalizableProxy(final byte[] serialized) {
        return new Proxy(serialized);
    }
}
