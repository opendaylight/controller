/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import java.io.DataInput;
import java.io.IOException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.AbstractIdentifiablePayload.AbstractProxy;

/**
 * Serialization proxy for {@link AbortTransactionPayload}.
 */
// FIXME: final when we eliminate legacy proxy
class AT extends AbstractProxy<TransactionIdentifier> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public AT() {
        // For Externalizable
    }

    AT(final byte[] serialized) {
        super(serialized);
    }

    @Override
    protected final TransactionIdentifier readIdentifier(final DataInput in) throws IOException {
        return TransactionIdentifier.readFrom(in);
    }

    @Override
    protected final AbortTransactionPayload createObject(final TransactionIdentifier identifier,
            final byte[] serialized) {
        return new AbortTransactionPayload(identifier, serialized);
    }
}
