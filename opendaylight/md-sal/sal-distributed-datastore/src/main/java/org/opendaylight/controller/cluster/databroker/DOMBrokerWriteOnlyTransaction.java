/*
 * Copyright (c) 2015 Huawei Technologies Co. Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.databroker;

import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

public class DOMBrokerWriteOnlyTransaction extends AbstractDOMBrokerWriteTransaction<DOMStoreWriteTransaction> {
    /**
     * Creates new composite Transactions.
     *
     * @param identifier
     *            Identifier of transaction.
     * @param storeTxFactories
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
