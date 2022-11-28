/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
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
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DisableTrackingPayload extends AbstractIdentifiablePayload<ClientIdentifier> {
    private static final class Proxy extends AbstractProxy<ClientIdentifier> {
        @java.io.Serial
        private static final long serialVersionUID = -5490519942445085251L;

        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final byte[] serialized) {
            super(serialized);
        }

        @Override
        protected ClientIdentifier readIdentifier(final DataInput in) throws IOException {
            return ClientIdentifier.readFrom(in);
        }

        @Override
        protected DisableTrackingPayload createObject(final ClientIdentifier identifier,
                final byte[] serialized) {
            return new DisableTrackingPayload(identifier, serialized);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(DisableTrackingPayload.class);
    private static final long serialVersionUID = 1L;
    private static final int PROXY_SIZE = externalizableProxySize(Proxy::new);

    DisableTrackingPayload(final ClientIdentifier clientId, final byte[] serialized) {
        super(clientId, serialized);
    }

    public static DisableTrackingPayload create(final ClientIdentifier clientId,
            final int initialSerializedBufferCapacity) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput(initialSerializedBufferCapacity);
        try {
            clientId.writeTo(out);
        } catch (IOException e) {
            // This should never happen
            LOG.error("Failed to serialize {}", clientId, e);
            throw new IllegalStateException("Failed to serialize " + clientId, e);
        }
        return new DisableTrackingPayload(clientId, out.toByteArray());
    }

    @Override
    protected Proxy externalizableProxy(final byte[] serialized) {
        return new Proxy(serialized);
    }

    @Override
    protected int externalizableProxySize() {
        return PROXY_SIZE;
    }
}
