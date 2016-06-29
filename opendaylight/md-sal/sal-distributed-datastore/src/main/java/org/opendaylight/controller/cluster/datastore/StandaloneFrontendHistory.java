/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

/**
 * Abstract class for providing logical tracking of frontend local histories. This class is specialized for
 * standalong transactions and chained transactions.
 *
 * @author Robert Varga
 */
final class StandaloneFrontendHistory extends AbstractFrontendHistory {
    private final LocalHistoryIdentifier identifier;
    private final ShardDataTree tree;

    StandaloneFrontendHistory(final String persistenceId, final ClientIdentifier clientId, final ShardDataTree tree) {
        super(persistenceId);
        this.identifier = new LocalHistoryIdentifier(clientId, 0);
        this.tree = Preconditions.checkNotNull(tree);
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    FrontendTransaction createOpenTransaction(final TransactionIdentifier id) throws RequestException {
        return FrontendTransaction.createOpen(this, tree.newReadWriteTransaction(id));
    }

    @Override
    FrontendTransaction createReadyTransaction(final TransactionIdentifier id, final DataTreeModification mod)
            throws RequestException {
        return FrontendTransaction.createReady(this, id, mod);
    }

    @Override
    ShardDataTreeCohort createReadyCohort(final TransactionIdentifier id, final DataTreeModification mod) {
        return tree.createReadyCohort(id, mod);
    }
}
