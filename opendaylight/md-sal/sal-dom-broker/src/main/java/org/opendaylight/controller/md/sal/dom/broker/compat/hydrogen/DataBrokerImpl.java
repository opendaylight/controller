/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.compat.hydrogen;

import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.impl.service.AbstractDataBroker;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.controller.sal.core.api.data.DataChangeListener;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.data.DataValidator;
import org.opendaylight.controller.sal.dom.broker.impl.DataReaderRouter;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import com.google.common.util.concurrent.MoreExecutors;

@Deprecated
public class DataBrokerImpl extends AbstractDataBroker<YangInstanceIdentifier, CompositeNode, DataChangeListener> implements
        DataProviderService, AutoCloseable {

    private AtomicLong nextTransaction = new AtomicLong();
    private final AtomicLong createdTransactionsCount = new AtomicLong();

    public DataBrokerImpl() {
        setDataReadRouter(new DataReaderRouter());
        setExecutor(MoreExecutors.sameThreadExecutor());
    }

    public AtomicLong getCreatedTransactionsCount() {
        return createdTransactionsCount;
    }

    @Override
    public DataTransactionImpl beginTransaction() {
        String transactionId = "DOM-" + nextTransaction.getAndIncrement();
        createdTransactionsCount.getAndIncrement();
        return new DataTransactionImpl(transactionId,this);
    }

    @Override
    public Registration registerConfigurationReader(
            YangInstanceIdentifier path, DataReader<YangInstanceIdentifier, CompositeNode> reader) {
        return getDataReadRouter().registerConfigurationReader(path, reader);
    }

    @Override
    public Registration registerOperationalReader(
            YangInstanceIdentifier path, DataReader<YangInstanceIdentifier, CompositeNode> reader) {
        return getDataReadRouter().registerOperationalReader(path, reader);
    }

    @Deprecated
    @Override
    public void addValidator(DataStoreIdentifier store, DataValidator validator) {
        throw new UnsupportedOperationException("Deprecated");

    }

    @Deprecated
    @Override
    public void removeValidator(DataStoreIdentifier store, DataValidator validator) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Deprecated
    @Override
    public void addRefresher(DataStoreIdentifier store, DataRefresher refresher) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Deprecated
    @Override
    public void removeRefresher(DataStoreIdentifier store, DataRefresher refresher) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public void close() throws Exception {

    }

}
