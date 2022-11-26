/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.base.Verify.verifyNotNull;

import java.io.DataInput;
import java.io.IOException;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.AbstractIdentifiablePayload.AbstractProxy;
import org.opendaylight.controller.cluster.datastore.utils.ImmutableUnsignedLongSet;

/**
 * Serialization proxy for {@link SkipTransactionsPayload}.
 */
final class ST extends AbstractProxy<LocalHistoryIdentifier> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private ImmutableUnsignedLongSet transactionIds;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public ST() {
        // For Externalizable
    }

    ST(final byte[] serialized) {
        super(serialized);
    }

    @Override
    protected LocalHistoryIdentifier readIdentifier(final DataInput in) throws IOException {
        final var id = LocalHistoryIdentifier.readFrom(in);
        transactionIds = ImmutableUnsignedLongSet.readFrom(in);
        return id;
    }

    @Override
    protected SkipTransactionsPayload createObject(final LocalHistoryIdentifier identifier, final byte[] serialized) {
        return new SkipTransactionsPayload(identifier, serialized, verifyNotNull(transactionIds));
    }

}
