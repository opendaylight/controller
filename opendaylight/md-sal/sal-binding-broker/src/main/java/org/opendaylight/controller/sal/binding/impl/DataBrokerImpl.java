/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.common.impl.service.AbstractDataBroker;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.impl.util.BindingAwareDataReaderRouter;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.DataRoot;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.DataObjectReadingUtil;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;


public class DataBrokerImpl extends AbstractDataBroker<InstanceIdentifier<? extends DataObject>, DataObject, DataChangeListener> //
       implements DataProviderService, AutoCloseable {

    private final static class ContainsWildcarded implements Predicate<InstanceIdentifier<? extends DataObject>> {

        private final  InstanceIdentifier<? extends DataObject> key;

        public ContainsWildcarded(InstanceIdentifier<? extends DataObject> key) {
            this.key = key;
        }

        @Override
        public boolean apply(InstanceIdentifier<? extends DataObject> input) {
            return key.containsWildcarded(input);
        }
    }

    private final static class IsContainedWildcarded implements Predicate<InstanceIdentifier<? extends DataObject>> {

        private final  InstanceIdentifier<? extends DataObject> key;

        public IsContainedWildcarded(InstanceIdentifier<? extends DataObject> key) {
            this.key = key;
        }

        @Override
        public boolean apply(InstanceIdentifier<? extends DataObject> input) {
            return input.containsWildcarded(key);
        }
    }

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
    protected Predicate<InstanceIdentifier<? extends DataObject>> createContainsPredicate(final
            InstanceIdentifier<? extends DataObject> key) {
        return new ContainsWildcarded(key);
    }

    @Override
    protected Predicate<InstanceIdentifier<? extends DataObject>> createIsContainedPredicate(final
            InstanceIdentifier<? extends DataObject> key) {
        return new IsContainedWildcarded(key);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected Map<InstanceIdentifier<? extends DataObject>, DataObject> deepGetBySubpath(
            Map<InstanceIdentifier<? extends DataObject>, DataObject> dataSet,
            InstanceIdentifier<? extends DataObject> path) {
        Builder<InstanceIdentifier<? extends DataObject>, DataObject> builder = ImmutableMap.builder();
        Map<InstanceIdentifier<? extends DataObject>, DataObject> potential = Maps.filterKeys(dataSet, createIsContainedPredicate(path));
        for(Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry : potential.entrySet()) {
            try {
                builder.putAll(DataObjectReadingUtil.readData(entry.getValue(),(InstanceIdentifier)entry.getKey(),path));
            } catch (Exception e) {
                // FIXME : Log exception;
            }
        }
        return builder.build();

    }

}
