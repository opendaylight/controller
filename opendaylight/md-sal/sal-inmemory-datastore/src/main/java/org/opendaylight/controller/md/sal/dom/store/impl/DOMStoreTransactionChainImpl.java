/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.sal.core.spi.data.AbstractSnapshotBackedTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;

final class DOMStoreTransactionChainImpl extends AbstractSnapshotBackedTransactionChain<String> {
    private final InMemoryDOMDataStore store;

    DOMStoreTransactionChainImpl(final InMemoryDOMDataStore store) {
        this.store = Preconditions.checkNotNull(store);
    }

    @Override
    protected DOMStoreThreePhaseCommitCohort createCohort(final SnapshotBackedWriteTransaction<String> tx, final DataTreeModification modification) {
        return new ChainedTransactionCommitImpl(store, tx, modification, this);
    }

    @Override
    protected DataTreeSnapshot takeSnapshot() {
        return store.takeSnapshot();
    }

    @Override
    protected String nextTransactionIdentifier() {
        return store.nextIdentifier();
    }

    @Override
    protected boolean getDebugTransactions() {
        return store.getDebugTransactions();
    }

    void transactionCommited(final SnapshotBackedWriteTransaction<String> transaction) {
        super.onTransactionCommited(transaction);
    }
}
