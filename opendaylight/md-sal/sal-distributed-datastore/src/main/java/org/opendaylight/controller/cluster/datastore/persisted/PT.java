/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.AbstractIdentifiablePayload.Proxy;

/**
 * Serialization proxy for {@link PurgeTransactionPayload}.
 */
final class PT implements Proxy {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private TransactionIdentifier identifier;
    private byte[] bytes;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public PT() {
        // For Externalizable
    }

    PT(final byte[] bytes) {
        this.bytes = requireNonNull(bytes);
    }

    @Override
    public byte[] bytes() {
        return bytes;
    }

    @Override
    public void readExternal(final byte[] bytes) throws IOException {
        this.bytes = requireNonNull(bytes);
        identifier = verifyNotNull(TransactionIdentifier.readFrom(ByteStreams.newDataInput(bytes)));
    }

    @Override
    public Object readResolve() {
        return new PurgeTransactionPayload(identifier, bytes);
    }
}
