/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import javax.annotation.concurrent.GuardedBy;
import java.util.Map;

public abstract class AbstractDOMDataBrokerDecorator implements DOMDataBroker,
        AutoCloseable, DOMDataCommitImplementation{
    protected  DOMDataBrokerImpl domDataBroker;

    public AbstractDOMDataBrokerDecorator(DOMDataBrokerImpl domDataBroker) {
        this.domDataBroker = domDataBroker;
    }

    public Object newTransactionIdentifier() {
        return domDataBroker.newTransactionIdentifier();
    }

    public Map<LogicalDatastoreType, DOMStore> getTxFactories() {
        return domDataBroker.getTxFactories();
    }

    @GuardedBy("this")
    public void checkNotClosed() {
        domDataBroker.checkNotClosed();
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit(DOMDataWriteTransaction transaction, Iterable<DOMStoreThreePhaseCommitCohort> cohorts) {
        return domDataBroker.submit(transaction, cohorts);
    }

    @Override
    public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(LogicalDatastoreType store, YangInstanceIdentifier path, DOMDataChangeListener listener, DataChangeScope triggeringScope) {
        return domDataBroker.registerDataChangeListener(store, path, listener, triggeringScope);
    }

    @Override
    public DOMTransactionChain createTransactionChain(TransactionChainListener listener) {
        return domDataBroker.createTransactionChain(listener);
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return domDataBroker.newReadOnlyTransaction();
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return domDataBroker.newWriteOnlyTransaction();
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return domDataBroker.newReadWriteTransaction();
    }

    @Override
    @GuardedBy("this")
    public void close() {
        domDataBroker.close();
    }
}
