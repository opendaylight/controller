/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.common.impl.service.AbstractDataBroker;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.impl.util.BindingAwareDataReaderRouter;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.DataRoot;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;


public class DataBrokerImpl extends AbstractDataBroker<InstanceIdentifier<? extends DataObject>, DataObject, DataChangeListener> //
       implements DataProviderService, AutoCloseable {

    private final AtomicLong nextTransaction = new AtomicLong();
    private final AtomicLong createdTransactionsCount = new AtomicLong();
    
    public AtomicLong getCreatedTransactionsCount() {
        return createdTransactionsCount;
    }

    public DataBrokerImpl() {
        setDataReadRouter(new BindingAwareDataReaderRouter());
    }

    @Override
    public DataTransactionImpl beginTransaction() {
        String transactionId = "BA-" + nextTransaction.getAndIncrement();
        createdTransactionsCount.getAndIncrement();
        return new DataTransactionImpl(transactionId,this);
    }

    @Override
    @Deprecated
    public <T extends DataRoot> T getData(DataStoreIdentifier store, Class<T> rootType) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    @Deprecated
    public <T extends DataRoot> T getData(DataStoreIdentifier store, T filter) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    @Deprecated
    public <T extends DataRoot> T getCandidateData(DataStoreIdentifier store, Class<T> rootType) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    @Deprecated
    public <T extends DataRoot> T getCandidateData(DataStoreIdentifier store, T filter) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    @Deprecated
    public RpcResult<DataRoot> editCandidateData(DataStoreIdentifier store, DataRoot changeSet) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    @Deprecated
    public Future<RpcResult<Void>> commit(DataStoreIdentifier store) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    @Deprecated
    public DataObject getData(InstanceIdentifier<? extends DataObject> data) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    @Deprecated
    public DataObject getConfigurationData(InstanceIdentifier<?> data) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    @Deprecated
    public void registerChangeListener(InstanceIdentifier<? extends DataObject> path, DataChangeListener changeListener) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    @Deprecated
    public void unregisterChangeListener(InstanceIdentifier<? extends DataObject> path,
            DataChangeListener changeListener) {
        throw new UnsupportedOperationException("Deprecated");
    }
    
    @Override
    public void close() throws Exception {
        
    }
    
    
    @Override
    protected boolean isAffectedBy(InstanceIdentifier<? extends DataObject> key,
            Set<InstanceIdentifier<? extends DataObject>> paths) {
        if (paths.contains(key)) {
            return true;
        }
        for (InstanceIdentifier<?> path : paths) {
            if (key.containsWildcarded(path)) {
                return true;
            }
        }
        return false;
    }
}
