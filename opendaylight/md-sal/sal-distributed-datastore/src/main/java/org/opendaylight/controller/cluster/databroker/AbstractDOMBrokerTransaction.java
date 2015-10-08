/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.databroker;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public abstract class AbstractDOMBrokerTransaction<T extends DOMStoreTransaction> implements
        AsyncTransaction<YangInstanceIdentifier, NormalizedNode<?, ?>> {

    private EnumMap<LogicalDatastoreType, T> backingTxs;
    private final Object identifier;
    private final Map<LogicalDatastoreType, ? extends DOMStoreTransactionFactory> storeTxFactories;

    /**
     *
     * Creates new composite Transactions.
     *
     * @param identifier
     *            Identifier of transaction.
     */
    protected AbstractDOMBrokerTransaction(final Object identifier, Map<LogicalDatastoreType, ? extends DOMStoreTransactionFactory> storeTxFactories) {
        this.identifier = Preconditions.checkNotNull(identifier, "Identifier should not be null");
        this.storeTxFactories = Preconditions.checkNotNull(storeTxFactories, "Store Transaction Factories should not be null");
        this.backingTxs = new EnumMap<>(LogicalDatastoreType.class);
    }

    /**
     * Returns subtransaction associated with supplied key.
     *
     * @param key
     * @return
     * @throws NullPointerException
     *             if key is null
     * @throws IllegalArgumentException
     *             if no subtransaction is associated with key.
     */
    protected final T getSubtransaction(final LogicalDatastoreType key) {
        Preconditions.checkNotNull(key, "key must not be null.");

        T ret = backingTxs.get(key);
        if(ret == null){
            ret = createTransaction(key);
            backingTxs.put(key, ret);
        }
        Preconditions.checkArgument(ret != null, "No subtransaction associated with %s", key);
        return ret;
    }

    protected abstract T createTransaction(final LogicalDatastoreType key);

    /**
     * Returns immutable Iterable of all subtransactions.
     *
     */
    protected Collection<T> getSubtransactions() {
        return backingTxs.values();
    }

    @Override
    public Object getIdentifier() {
        return identifier;
    }

    protected void closeSubtransactions() {
        /*
         * We share one exception for all failures, which are added
         * as supressedExceptions to it.
         */
        IllegalStateException failure = null;
        for (T subtransaction : backingTxs.values()) {
            try {
                subtransaction.close();
            } catch (Exception e) {
                // If we did not allocated failure we allocate it
                if (failure == null) {
                    failure = new IllegalStateException("Uncaught exception occured during closing transaction", e);
                } else {
                    // We update it with additional exceptions, which occurred during error.
                    failure.addSuppressed(e);
                }
            }
        }
        // If we have failure, we throw it at after all attempts to close.
        if (failure != null) {
            throw failure;
        }
    }

    protected DOMStoreTransactionFactory getTxFactory(LogicalDatastoreType type){
        return storeTxFactories.get(type);
    }
}
