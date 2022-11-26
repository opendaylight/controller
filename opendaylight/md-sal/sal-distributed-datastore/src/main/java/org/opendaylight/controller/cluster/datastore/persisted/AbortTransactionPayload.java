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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payload persisted when a transaction is aborted. It contains the transaction identifier.
 *
 * @author Robert Varga
 */
public final class AbortTransactionPayload extends AbstractIdentifiablePayload<TransactionIdentifier> {
    private static final class Proxy extends AbstractProxy<TransactionIdentifier> {
        private static final long serialVersionUID = 1L;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
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
            return new AbortTransactionPayload(true, identifier, serialized);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbortTransactionPayload.class);
    private static final long serialVersionUID = 1L;
    private static final int PROXY_SIZE = externalizableProxySize(AT::new);
    private static final int LEGACY_PROXY_SIZE = externalizableProxySize(Proxy::new);

    AbortTransactionPayload(final boolean legacy, final TransactionIdentifier transactionId, final byte[] serialized) {
        super(legacy, transactionId, serialized);
    }

    public static AbortTransactionPayload create(final PayloadVersion version,
            final TransactionIdentifier transactionId, final int initialSerializedBufferCapacity) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput(initialSerializedBufferCapacity);
        try {
            transactionId.writeTo(out);
        } catch (IOException e) {
            // This should never happen
            LOG.error("Failed to serialize {}", transactionId, e);
            throw new IllegalStateException("Failed to serialized " + transactionId, e);
        }
        return new AbortTransactionPayload(PayloadVersion.MAGNESIUM.lte(version), transactionId, out.toByteArray());
    }

    @Override
    protected AT proxy(final byte[] serialized) {
        return new AT(serialized);
    }

    @Override
    protected int proxySize() {
        return PROXY_SIZE;
    }

    @Override
    protected Proxy legacyProxy(final byte[] serialized) {
        return new Proxy(serialized);
    }

    @Override
    protected int legacyProxySize() {
        return LEGACY_PROXY_SIZE;
    }
}
