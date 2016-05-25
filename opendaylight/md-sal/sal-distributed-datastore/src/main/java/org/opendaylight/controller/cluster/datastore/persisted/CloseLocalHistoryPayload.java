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
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

/**
 * Payload persisted when a local history is closed cleanly. It contains a {@link LocalHistoryIdentifier}.
 *
 * @author Robert Varga
 */
public final class CloseLocalHistoryPayload extends AbstractIdentifiablePayload<LocalHistoryIdentifier> {
    private static final class Proxy extends AbstractProxy<LocalHistoryIdentifier> {
        private static final long serialVersionUID = 1L;

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
        protected CloseLocalHistoryPayload createObject(final LocalHistoryIdentifier identifier,
                final byte[] serialized) {
            return new CloseLocalHistoryPayload(identifier, serialized);
        }
    }

    private static final long serialVersionUID = 1L;

    CloseLocalHistoryPayload(final LocalHistoryIdentifier historyId, final byte[] serialized) {
        super(historyId, serialized);
    }

    public static CloseLocalHistoryPayload create(final LocalHistoryIdentifier historyId) throws IOException {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        historyId.writeTo(out);
        return new CloseLocalHistoryPayload(historyId, out.toByteArray());
    }

    @Override
    protected Proxy externalizableProxy(final byte[] serialized) {
        return new Proxy(serialized);
    }
}
