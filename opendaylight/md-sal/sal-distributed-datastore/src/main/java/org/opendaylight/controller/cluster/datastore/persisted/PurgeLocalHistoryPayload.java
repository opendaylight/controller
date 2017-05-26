/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.base.Throwables;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.DataInput;
import java.io.IOException;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payload persisted when a local history is completely purged, i.e. the frontend has removed it from its tracking.
 * It contains a {@link LocalHistoryIdentifier}.
 *
 * @author Robert Varga
 */
public final class PurgeLocalHistoryPayload extends AbstractIdentifiablePayload<LocalHistoryIdentifier> {
    private static final class Proxy extends AbstractProxy<LocalHistoryIdentifier> {
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
        protected LocalHistoryIdentifier readIdentifier(final DataInput in) throws IOException {
            return LocalHistoryIdentifier.readFrom(in);
        }

        @Override
        protected PurgeLocalHistoryPayload createObject(final LocalHistoryIdentifier identifier,
                final byte[] serialized) {
            return new PurgeLocalHistoryPayload(identifier, serialized);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(PurgeLocalHistoryPayload.class);
    private static final long serialVersionUID = 1L;

    PurgeLocalHistoryPayload(final LocalHistoryIdentifier historyId, final byte[] serialized) {
        super(historyId, serialized);
    }

    public static PurgeLocalHistoryPayload create(final LocalHistoryIdentifier historyId) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        try {
            historyId.writeTo(out);
        } catch (IOException e) {
            // This should never happen
            LOG.error("Failed to serialize {}", historyId, e);
            throw Throwables.propagate(e);
        }
        return new PurgeLocalHistoryPayload(historyId, out.toByteArray());
    }

    @Override
    protected Proxy externalizableProxy(final byte[] serialized) {
        return new Proxy(serialized);
    }
}
