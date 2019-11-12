/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.annotations.Beta;
import com.google.common.primitives.UnsignedLong;
import java.util.List;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payload persisted when a local history is instructed some transaction identifiers, i.e. the frontend has used them
 * for other purposes. It contains a {@link LocalHistoryIdentifier} and a list of transaction identifiers within that
 * local history.
 */
@Beta
public final class SkipTransactionsLocalHistoryPayload extends AbstractIdentifiablePayload<LocalHistoryIdentifier> {
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


    }

    private static final Logger LOG = LoggerFactory.getLogger(SkipTransactionsLocalHistoryPayload.class);
    private static final long serialVersionUID = 1L;

    SkipTransactionsLocalHistoryPayload(final LocalHistoryIdentifier historyId, final byte[] serialized) {
        super(historyId, serialized);
    }

    public static PurgeLocalHistoryPayload create(final LocalHistoryIdentifier historyId,
            final List<UnsignedLong> transactionIds, final int initialSerializedBufferCapacity) {


    }

    @Override
    protected Proxy externalizableProxy(final byte[] serialized) {
        return new Proxy(serialized);
    }
}
