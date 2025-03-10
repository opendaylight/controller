/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedLong;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.raft.spi.MutableUnsignedLongSet;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;

/**
 * Standalone transaction specialization of {@link AbstractFrontendHistory}. There can be multiple open transactions
 * and they are submitted in any order.
 */
final class StandaloneFrontendHistory extends AbstractFrontendHistory {
    private final @NonNull LocalHistoryIdentifier identifier;
    private final @NonNull ShardDataTree tree;

    private StandaloneFrontendHistory(final String persistenceId, final ClientIdentifier clientId,
            final ShardDataTree tree, final Map<UnsignedLong, Boolean> closedTransactions,
            final MutableUnsignedLongSet purgedTransactions) {
        super(persistenceId, tree, closedTransactions, purgedTransactions);
        this.tree = requireNonNull(tree);
        identifier = identifierForClient(clientId);
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

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    FrontendTransaction createOpenSnapshot(final TransactionIdentifier id) {
        return FrontendReadOnlyTransaction.create(this, tree.newStandaloneReadOnlyTransaction(id));
    }

    @Override
    FrontendTransaction createOpenTransaction(final TransactionIdentifier id) {
        return FrontendReadWriteTransaction.createOpen(this, tree.newStandaloneReadWriteTransaction(id));
    }

    @Override
    FrontendTransaction createReadyTransaction(final TransactionIdentifier id, final DataTreeModification mod) {
        return FrontendReadWriteTransaction.createReady(this, id, mod);
    }

    @Override
    CommitCohort createFailedCohort(final TransactionIdentifier id, final DataTreeModification mod,
            final Exception failure) {
        return tree.createFailedCohort(id, mod, failure);
    }

    @Override
    CommitCohort createReadyCohort(final TransactionIdentifier id, final DataTreeModification mod,
            final Optional<SortedSet<String>> participatingShardNames) {
        return tree.createReadyCohort(id, mod, participatingShardNames);
    }
}
