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
import java.util.SortedSet;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.raft.spi.MutableUnsignedLongSet;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;

/**
 * Chained transaction specialization of {@link AbstractFrontendHistory}. It prevents concurrent open transactions.
 *
 * @author Robert Varga
 */
final class LocalFrontendHistory extends AbstractFrontendHistory {
    private final ShardDataTreeTransactionChain chain;

    private LocalFrontendHistory(final String persistenceId, final ShardDataTree tree,
            final ShardDataTreeTransactionChain chain, final Map<UnsignedLong, Boolean> closedTransactions,
            final MutableUnsignedLongSet purgedTransactions) {
        super(persistenceId, tree, closedTransactions, purgedTransactions);
        this.chain = requireNonNull(chain);
    }

    static LocalFrontendHistory create(final String persistenceId, final ShardDataTree tree,
            final ShardDataTreeTransactionChain chain) {
        return new LocalFrontendHistory(persistenceId, tree, chain, ImmutableMap.of(), MutableUnsignedLongSet.of());
    }

    static LocalFrontendHistory recreate(final String persistenceId, final ShardDataTree tree,
            final ShardDataTreeTransactionChain chain, final Map<UnsignedLong, Boolean> closedTransactions,
            final MutableUnsignedLongSet purgedTransactions) {
        return new LocalFrontendHistory(persistenceId, tree, chain, new HashMap<>(closedTransactions),
            purgedTransactions.mutableCopy());
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return chain.getIdentifier();
    }

    @Override
    FrontendTransaction createOpenSnapshot(final TransactionIdentifier id) {
        return FrontendReadOnlyTransaction.create(this, chain.newReadOnlyTransaction(id));
    }

    @Override
    FrontendTransaction createOpenTransaction(final TransactionIdentifier id) {
        return FrontendReadWriteTransaction.createOpen(this, chain.newReadWriteTransaction(id));
    }

    @Override
    FrontendTransaction createReadyTransaction(final TransactionIdentifier id, final DataTreeModification mod) {
        return FrontendReadWriteTransaction.createReady(this, id, mod);
    }

    @Override
    CommitCohort createFailedCohort(final TransactionIdentifier id, final DataTreeModification mod,
            final Exception failure) {
        return chain.createFailedCohort(id, mod, failure);
    }

    @Override
    CommitCohort createReadyCohort(final TransactionIdentifier id, final DataTreeModification mod,
            final SortedSet<String> participatingShardNames) {
        return chain.createReadyCohort(id, mod, participatingShardNames);
    }
}
