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
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.raft.spi.MutableUnsignedLongSet;

/**
 * Standalone transaction specialization of {@link AbstractFrontendHistory}. There can be multiple open transactions
 * and they are submitted in any order.
 */
final class StandaloneFrontendHistory extends AbstractFrontendHistory {
    private StandaloneFrontendHistory(final String persistenceId, final ClientIdentifier clientId,
            final ShardDataTree tree, final Map<UnsignedLong, Boolean> closedTransactions,
            final MutableUnsignedLongSet purgedTransactions) {
        super(persistenceId, identifierForClient(clientId), tree.unorderedParent(), closedTransactions,
            purgedTransactions);
    }

    static @NonNull LocalHistoryIdentifier identifierForClient(final ClientIdentifier clientId) {
        return new LocalHistoryIdentifier(clientId, 0);
    }

    static @NonNull StandaloneFrontendHistory create(final String persistenceId, final ClientIdentifier clientId,
            final ShardDataTree tree) {
        return new StandaloneFrontendHistory(persistenceId, clientId, tree, ImmutableMap.of(),
            MutableUnsignedLongSet.of());
    }

    static @NonNull StandaloneFrontendHistory recreate(final String persistenceId, final ClientIdentifier clientId,
            final ShardDataTree tree, final Map<UnsignedLong, Boolean> closedTransactions,
            final MutableUnsignedLongSet purgedTransactions) {
        return new StandaloneFrontendHistory(persistenceId, clientId, tree, new HashMap<>(closedTransactions),
            purgedTransactions.mutableCopy());
    }
}
