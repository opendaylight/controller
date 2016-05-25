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
import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * Payload persisted when a local history is created. It contains a {@link LocalHistoryIdentifier}.
 *
 * @author Robert Varga
 */
public final class CreateLocalHistoryPayload extends AbstractIdentifiablePayload<LocalHistoryIdentifier> {
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
        protected Identifiable<LocalHistoryIdentifier> createObject(final LocalHistoryIdentifier identifier,
                final byte[] serialized) {
            return new CreateLocalHistoryPayload(identifier, serialized);
        }
    }

    private static final long serialVersionUID = 1L;

    CreateLocalHistoryPayload(final LocalHistoryIdentifier historyId, final byte[] serialized) {
        super(historyId, serialized);
    }

    public static CreateLocalHistoryPayload create(final LocalHistoryIdentifier historyId) throws IOException {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        historyId.writeTo(out);
        return new CreateLocalHistoryPayload(historyId, out.toByteArray());
    }

    @Override
    protected AbstractProxy<LocalHistoryIdentifier> externalizableProxy(final byte[] serialized) {
        return new Proxy(serialized);
    }
}
