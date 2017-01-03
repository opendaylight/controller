/*
 * Copyright (c) 2015 Huawei Technologies Co. Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.databroker;

import java.util.Map;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionFactory;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;

public class DOMBrokerWriteOnlyTransaction extends AbstractDOMBrokerWriteTransaction<DOMStoreWriteTransaction> {

    /**
     * Constructs an instance.
     *
     * @param identifier identifier of transaction.
     * @param storeTxFactories the backing transaction store factories
     */
    public DOMBrokerWriteOnlyTransaction(Object identifier,
            Map<LogicalDatastoreType, ? extends DOMStoreTransactionFactory> storeTxFactories,
            AbstractDOMTransactionFactory<?> commitImpl) {
        super(identifier, storeTxFactories, commitImpl);
    }

    @Override
    protected DOMStoreWriteTransaction createTransaction(LogicalDatastoreType key) {
        return getTxFactory(key).newWriteOnlyTransaction();
    }

}
