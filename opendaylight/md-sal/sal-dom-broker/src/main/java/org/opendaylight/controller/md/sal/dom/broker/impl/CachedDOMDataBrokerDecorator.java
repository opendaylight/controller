/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;


import com.google.common.collect.ImmutableMap;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.store.impl.CachedInMemoryDataStoreDecorator;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.util.Map;

public class CachedDOMDataBrokerDecorator extends AbstractDOMDataBrokerDecorator {

    private ImmutableMap<LogicalDatastoreType, DOMStore> cachedDatastores;

    public CachedDOMDataBrokerDecorator(DOMDataBrokerImpl domDataBroker, YangInstanceIdentifier cachingPathNormalized) {
        super(domDataBroker);
        ImmutableMap.Builder<LogicalDatastoreType, DOMStore> builder = ImmutableMap.builder();
        for (Map.Entry<LogicalDatastoreType, DOMStore> store : getTxFactories().entrySet()) {
            if (store.getValue() instanceof InMemoryDOMDataStore) {
                builder.put(store.getKey(), new CachedInMemoryDataStoreDecorator(
                        (InMemoryDOMDataStore) store.getValue(), cachingPathNormalized));
            }
        }
       // TODO: throw exception when no suitable data stores are found
        cachedDatastores = builder.build();
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        checkNotClosed();
        ImmutableMap.Builder<LogicalDatastoreType, DOMStoreReadWriteTransaction> builder = ImmutableMap.builder();
        for (Map.Entry<LogicalDatastoreType, DOMStore> store : cachedDatastores.entrySet()) {
                builder.put(store.getKey(), store.getValue().newReadWriteTransaction());
        }
        return new DOMForwardedReadWriteTransaction(newTransactionIdentifier(), builder.build(), this);
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        checkNotClosed();
        ImmutableMap.Builder<LogicalDatastoreType, DOMStoreWriteTransaction> builder = ImmutableMap.builder();
        for (Map.Entry<LogicalDatastoreType, DOMStore> store : cachedDatastores.entrySet()) {
                builder.put(store.getKey(), store.getValue().newReadWriteTransaction());
        }
        return new DOMForwardedWriteTransaction<DOMStoreWriteTransaction>(newTransactionIdentifier(), builder.build(),
                this);
    }
}
