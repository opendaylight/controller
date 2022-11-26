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
    private static final Logger LOG = LoggerFactory.getLogger(PurgeLocalHistoryPayload.class);
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    private static final int PROXY_SIZE = externalizableProxySize(PH::new);

    PurgeLocalHistoryPayload(final LocalHistoryIdentifier historyId, final byte[] serialized) {
        super(historyId, serialized);
    }

    public static PurgeLocalHistoryPayload create(final LocalHistoryIdentifier historyId,
            final int initialSerializedBufferCapacity) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput(initialSerializedBufferCapacity);
        try {
            historyId.writeTo(out);
        } catch (IOException e) {
            // This should never happen
            LOG.error("Failed to serialize {}", historyId, e);
            throw new IllegalStateException("Failed to serialize " + historyId, e);
        }
        return new PurgeLocalHistoryPayload(historyId, out.toByteArray());
    }

    @Override
    protected PH externalizableProxy(final byte[] serialized) {
        return new PH(serialized);
    }

    @Override
    protected int externalizableProxySize() {
        return PROXY_SIZE;
    }
}
