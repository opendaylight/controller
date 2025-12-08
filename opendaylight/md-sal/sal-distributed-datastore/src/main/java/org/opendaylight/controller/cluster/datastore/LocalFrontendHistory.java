/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedLong;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.spi.MutableUnsignedLongSet;

/**
 * Chained transaction specialization of {@link AbstractFrontendHistory}. It prevents concurrent open transactions.
 */
final class LocalFrontendHistory extends AbstractFrontendHistory {
    private LocalFrontendHistory(final String persistenceId, final ChainedTransactionParent parent,
            final Map<UnsignedLong, Boolean> closedTransactions, final MutableUnsignedLongSet purgedTransactions) {
        super(persistenceId, parent.getIdentifier(), parent, closedTransactions, purgedTransactions);
    }

    static @NonNull LocalFrontendHistory create(final String persistenceId, final ChainedTransactionParent parent) {
        return new LocalFrontendHistory(persistenceId, parent, ImmutableMap.of(), MutableUnsignedLongSet.of());
    }

    static @NonNull LocalFrontendHistory recreate(final String persistenceId, final ChainedTransactionParent parent,
            final Map<UnsignedLong, Boolean> closedTransactions, final MutableUnsignedLongSet purgedTransactions) {
        return new LocalFrontendHistory(persistenceId, parent, new HashMap<>(closedTransactions),
            purgedTransactions.mutableCopy());
    }
}
