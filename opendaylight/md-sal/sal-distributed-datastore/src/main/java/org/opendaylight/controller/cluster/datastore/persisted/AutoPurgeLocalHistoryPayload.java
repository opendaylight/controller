/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
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
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payload persisted when a local history has been closed and we will not get another history event from the frontend.
 * It contains a {@link LocalHistoryIdentifier}.
 *
 * <p>
 * This differs from {@link PurgeLocalHistoryPayload}, which is an immediate and final decision to forget a history,
 * whereas this payload is an instruction to forget a history only after all of its tracked transactions have been
 * accounted for.
 *
 * <p>
 * This is only used with ask-based protocol. Tell-based protocol has an explicit finalization message, hence it issues
 * a {@link PurgeLocalHistoryPayload} instead.
 *
 * @author Robert Varga
 */
public final class AutoPurgeLocalHistoryPayload extends AbstractIdentifiablePayload<LocalHistoryIdentifier> {
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
        protected AutoPurgeLocalHistoryPayload createObject(final LocalHistoryIdentifier identifier,
                final byte[] serialized) {
            return new AutoPurgeLocalHistoryPayload(identifier, serialized);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AutoPurgeLocalHistoryPayload.class);
    private static final long serialVersionUID = 1L;

    AutoPurgeLocalHistoryPayload(final LocalHistoryIdentifier historyId, final byte[] serialized) {
        super(historyId, serialized);
    }

    public static AutoPurgeLocalHistoryPayload create(final LocalHistoryIdentifier historyId) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        try {
            historyId.writeTo(out);
        } catch (IOException e) {
            // This should never happen
            LOG.error("Failed to serialize {}", historyId, e);
            throw new RuntimeException("Failed to serialize " + historyId, e);
        }
        return new AutoPurgeLocalHistoryPayload(historyId, out.toByteArray());
    }

    @Override
    protected Proxy externalizableProxy(final byte[] serialized) {
        return new Proxy(serialized);
    }
}
