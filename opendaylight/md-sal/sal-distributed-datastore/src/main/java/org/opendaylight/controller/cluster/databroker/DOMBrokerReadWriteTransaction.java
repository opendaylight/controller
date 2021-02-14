/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionFactory;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DOMBrokerReadWriteTransaction extends AbstractDOMBrokerWriteTransaction<DOMStoreReadWriteTransaction>
        implements DOMDataTreeReadWriteTransaction {

    /**
     * Constructs an instance.
     *
     * @param identifier identifier of transaction.
     * @param storeTxFactories the backing transaction store factories
     */
    protected DOMBrokerReadWriteTransaction(final Object identifier,
            final Map<LogicalDatastoreType, ? extends DOMStoreTransactionFactory>  storeTxFactories,
            final AbstractDOMTransactionFactory<?> commitImpl) {
        super(identifier, storeTxFactories, commitImpl);
    }

    @Override
    public FluentFuture<Optional<NormalizedNode>> read(final LogicalDatastoreType store,
            final YangInstanceIdentifier path) {
        return getSubtransaction(store).read(path);
    }

    @Override
    public FluentFuture<Boolean> exists(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        return getSubtransaction(store).exists(path);
    }

    @Override
    protected DOMStoreReadWriteTransaction createTransaction(final LogicalDatastoreType key) {
        return getTxFactory(key).newReadWriteTransaction();
    }
}
