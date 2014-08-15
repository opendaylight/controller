/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.cached.store.impl;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.SnapshotBackedWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

// abstract decorator class that can be used by InMemoryDOMDataStore decorators
public abstract class InMemoryDOMDataStoreAbstractDecorator implements DOMStore, Identifiable<String>, SchemaContextListener,
        SnapshotBackedWriteTransaction.TransactionReadyPrototype,AutoCloseable {
    InMemoryDOMDataStore domDataStoreToBeDecorated;

    public InMemoryDOMDataStoreAbstractDecorator(InMemoryDOMDataStore domDataStoreToBeDecorated) {
        this.domDataStoreToBeDecorated = domDataStoreToBeDecorated;
    }

    @Override
    public <L extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L> registerChangeListener(YangInstanceIdentifier path, L listener, AsyncDataBroker.DataChangeScope scope) {
        return domDataStoreToBeDecorated.registerChangeListener(path, listener, scope);
    }

    @Override
    public DOMStoreTransactionChain createTransactionChain() {
        return domDataStoreToBeDecorated.createTransactionChain();
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return domDataStoreToBeDecorated.newReadOnlyTransaction();
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return domDataStoreToBeDecorated.newWriteOnlyTransaction();
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return domDataStoreToBeDecorated.newReadWriteTransaction();
    }

    @Override
    public String getIdentifier() {
        return domDataStoreToBeDecorated.getIdentifier();
    }

    @Override
    public void close() {
        domDataStoreToBeDecorated.close();
    }

    @Override
    public void onGlobalContextUpdated(SchemaContext ctx) {
        domDataStoreToBeDecorated.onGlobalContextUpdated(ctx);
    }

    @Override
    public DOMStoreThreePhaseCommitCohort ready(SnapshotBackedWriteTransaction writeTx) {
        return domDataStoreToBeDecorated.ready(writeTx);
    }
}
