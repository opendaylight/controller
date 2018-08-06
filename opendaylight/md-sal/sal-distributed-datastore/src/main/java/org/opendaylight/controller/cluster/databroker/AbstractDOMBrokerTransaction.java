/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionFactory;

public abstract class AbstractDOMBrokerTransaction<T extends DOMStoreTransaction> implements DOMDataTreeTransaction {

    private final EnumMap<LogicalDatastoreType, T> backingTxs;
    private final Object identifier;
    private final Map<LogicalDatastoreType, ? extends DOMStoreTransactionFactory> storeTxFactories;

    /**
     * Creates new composite Transactions.
     *
     * @param identifier Identifier of transaction.
     */
    protected AbstractDOMBrokerTransaction(final Object identifier,
            Map<LogicalDatastoreType, ? extends DOMStoreTransactionFactory> storeTxFactories) {
        this.identifier = requireNonNull(identifier, "Identifier should not be null");
        this.storeTxFactories = requireNonNull(storeTxFactories, "Store Transaction Factories should not be null");
        this.backingTxs = new EnumMap<>(LogicalDatastoreType.class);
    }

    /**
     * Returns subtransaction associated with supplied key.
     *
     * @param key the data store type key
     * @return the subtransaction
     * @throws NullPointerException
     *             if key is null
     * @throws IllegalArgumentException
     *             if no subtransaction is associated with key.
     */
    protected final T getSubtransaction(final LogicalDatastoreType key) {
        requireNonNull(key, "key must not be null.");

        T ret = backingTxs.get(key);
        if (ret == null) {
            ret = createTransaction(key);
            backingTxs.put(key, ret);
        }
        checkArgument(ret != null, "No subtransaction associated with %s", key);
        return ret;
    }

    protected abstract T createTransaction(LogicalDatastoreType key);

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

    @SuppressWarnings("checkstyle:IllegalCatch")
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

    protected DOMStoreTransactionFactory getTxFactory(LogicalDatastoreType type) {
        return storeTxFactories.get(type);
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this).omitNullValues()).toString();
    }

    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("identifier", identifier);
    }
}
