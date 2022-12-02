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
import org.opendaylight.controller.cluster.raft.persisted.LegacySerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payload persisted when a transaction is purged from the frontend. It contains the transaction identifier.
 *
 * @author Robert Varga
 */
public sealed class PurgeTransactionPayload extends AbstractIdentifiablePayload<TransactionIdentifier> {
    @Deprecated(since = "7.0.0", forRemoval = true)
    private static final class Magnesium extends PurgeTransactionPayload implements LegacySerializable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        Magnesium(final TransactionIdentifier transactionId, final byte[] serialized) {
            super(transactionId, serialized);
        }
    }

    @Deprecated(since = "7.0.0", forRemoval = true)
    private static final class Proxy extends AbstractProxy<TransactionIdentifier> {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        @Override
        protected TransactionIdentifier readIdentifier(final DataInput in) throws IOException {
            return TransactionIdentifier.readFrom(in);
        }

        @Override
        protected PurgeTransactionPayload createObject(final TransactionIdentifier identifier,
                final byte[] serialized) {
            return new Magnesium(identifier, serialized);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(PurgeTransactionPayload.class);
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    private static final int PROXY_SIZE = externalizableProxySize(PT::new);

    PurgeTransactionPayload(final TransactionIdentifier transactionId, final byte[] serialized) {
        super(transactionId, serialized);
    }

    public static PurgeTransactionPayload create(final TransactionIdentifier transactionId,
            final int initialSerializedBufferCapacity) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput(initialSerializedBufferCapacity);
        try {
            transactionId.writeTo(out);
        } catch (IOException e) {
            // This should never happen
            LOG.error("Failed to serialize {}", transactionId, e);
            throw new IllegalStateException("Failed to serialize " + transactionId, e);
        }
        return new PurgeTransactionPayload(transactionId, out.toByteArray());
    }

    @Override
    protected PT externalizableProxy(final byte[] serialized) {
        return new PT(serialized);
    }

    @Override
    protected int externalizableProxySize() {
        return PROXY_SIZE;
    }
}
