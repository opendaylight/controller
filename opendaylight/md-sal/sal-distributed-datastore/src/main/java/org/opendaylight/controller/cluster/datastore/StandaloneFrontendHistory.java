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
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

/**
 * Standalone transaction specialization of {@link AbstractFrontendHistory}. There can be multiple open transactions
 * and they are submitted in any order.
 *
 * @author Robert Varga
 */
final class StandaloneFrontendHistory extends AbstractFrontendHistory {
    private final LocalHistoryIdentifier identifier;
    private final ShardDataTree tree;

    private StandaloneFrontendHistory(final String persistenceId, final ClientIdentifier clientId,
            final ShardDataTree tree, final Map<UnsignedLong, Boolean> closedTransactions,
            final RangeSet<UnsignedLong> purgedTransactions) {
        super(persistenceId, tree, closedTransactions, purgedTransactions);
        this.identifier = new LocalHistoryIdentifier(clientId, 0);
        this.tree = Preconditions.checkNotNull(tree);
    }

    static StandaloneFrontendHistory create(final String persistenceId, final ClientIdentifier clientId,
            final ShardDataTree tree) {
        return new StandaloneFrontendHistory(persistenceId, clientId, tree, ImmutableMap.of(),
            TreeRangeSet.create());
    }

    static StandaloneFrontendHistory recreate(final String persistenceId, final ClientIdentifier clientId,
            final ShardDataTree tree, final Map<UnsignedLong, Boolean> closedTransactions,
            final RangeSet<UnsignedLong> purgedTransactions) {
        return new StandaloneFrontendHistory(persistenceId, clientId, tree, new HashMap<>(closedTransactions),
            purgedTransactions);
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    FrontendTransaction createOpenSnapshot(final TransactionIdentifier id) throws RequestException {
        return FrontendReadOnlyTransaction.create(this, tree.newReadOnlyTransaction(id));
    }

    @Override
    FrontendTransaction createOpenTransaction(final TransactionIdentifier id) throws RequestException {
        return FrontendReadWriteTransaction.createOpen(this, tree.newReadWriteTransaction(id));
    }

    @Override
    FrontendTransaction createReadyTransaction(final TransactionIdentifier id, final DataTreeModification mod)
            throws RequestException {
        return FrontendReadWriteTransaction.createReady(this, id, mod);
    }

    @Override
    ShardDataTreeCohort createFailedCohort(final TransactionIdentifier id, final DataTreeModification mod,
            final Exception failure) {
        return tree.createFailedCohort(id, mod, failure);
    }

    @Override
    ShardDataTreeCohort createReadyCohort(final TransactionIdentifier id, final DataTreeModification mod) {
        return tree.createReadyCohort(id, mod);
    }
}
