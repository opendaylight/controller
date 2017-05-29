/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.primitives.UnsignedLong;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;


/**
 * Chained transaction specialization of {@link AbstractFrontendHistory}. It prevents concurrent open transactions.
 *
 * @author Robert Varga
 */
final class LocalFrontendHistory extends AbstractFrontendHistory {
    private final ShardDataTreeTransactionChain chain;

    private LocalFrontendHistory(final String persistenceId, final ShardDataTree tree,
            final ShardDataTreeTransactionChain chain, final Map<UnsignedLong, Boolean> closedTransactions,
            final RangeSet<UnsignedLong> purgedTransactions) {
        super(persistenceId, tree, closedTransactions, purgedTransactions);
        this.chain = Preconditions.checkNotNull(chain);
    }

    static LocalFrontendHistory create(final String persistenceId, final ShardDataTree tree,
            final ShardDataTreeTransactionChain chain) {
        return new LocalFrontendHistory(persistenceId, tree, chain, ImmutableMap.of(), TreeRangeSet.create());
    }

    static LocalFrontendHistory recreate(final String persistenceId, final ShardDataTree tree,
            final ShardDataTreeTransactionChain chain, final Map<UnsignedLong, Boolean> closedTransactions,
            final RangeSet<UnsignedLong> purgedTransactions) {
        return new LocalFrontendHistory(persistenceId, tree, chain, new HashMap<>(closedTransactions),
            TreeRangeSet.create(purgedTransactions));
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return chain.getIdentifier();
    }

    @Override
    FrontendTransaction createOpenSnapshot(final TransactionIdentifier id) throws RequestException {
        return FrontendReadOnlyTransaction.create(this, chain.newReadOnlyTransaction(id));
    }

    @Override
    FrontendTransaction createOpenTransaction(final TransactionIdentifier id) throws RequestException {
        return FrontendReadWriteTransaction.createOpen(this, chain.newReadWriteTransaction(id));
    }

    @Override
    FrontendTransaction createReadyTransaction(final TransactionIdentifier id, final DataTreeModification mod)
            throws RequestException {
        return FrontendReadWriteTransaction.createReady(this, id, mod);
    }

    @Override
    ShardDataTreeCohort createFailedCohort(final TransactionIdentifier id, final DataTreeModification mod,
            final Exception failure) {
        return chain.createFailedCohort(id, mod, failure);
    }

    @Override
    ShardDataTreeCohort createReadyCohort(final TransactionIdentifier id, final DataTreeModification mod) {
        return chain.createReadyCohort(id, mod);
    }
}
