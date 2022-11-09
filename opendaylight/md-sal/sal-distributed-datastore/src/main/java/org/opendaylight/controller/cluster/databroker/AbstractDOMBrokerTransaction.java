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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionDatastoreMismatchException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionFactory;

public abstract class AbstractDOMBrokerTransaction<T extends DOMStoreTransaction> implements DOMDataTreeTransaction {

    private static final VarHandle BACKING_TX;

    static {
        try {
            BACKING_TX = MethodHandles.lookup()
                .findVarHandle(AbstractDOMBrokerTransaction.class, "backingTx", Entry.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final @NonNull Object identifier;
    private final Map<LogicalDatastoreType, ? extends DOMStoreTransactionFactory> storeTxFactories;

    private volatile Entry<LogicalDatastoreType, T> backingTx;

    /**
     * Creates new transaction.
     *
     * @param identifier Identifier of transaction.
     */
    protected AbstractDOMBrokerTransaction(final Object identifier,
            Map<LogicalDatastoreType, ? extends DOMStoreTransactionFactory> storeTxFactories) {
        this.identifier = requireNonNull(identifier, "Identifier should not be null");
        this.storeTxFactories = requireNonNull(storeTxFactories, "Store Transaction Factories should not be null");
        checkArgument(!storeTxFactories.isEmpty(), "Store Transaction Factories should not be empty");
    }

    /**
     * Returns sub-transaction associated with supplied key.
     *
     * @param datastoreType the data store type
     * @return the sub-transaction
     * @throws NullPointerException                  if datastoreType is null
     * @throws IllegalArgumentException              if no sub-transaction is associated with datastoreType.
     * @throws TransactionDatastoreMismatchException if datastoreType mismatches the one used at first access
     */
    protected final T getSubtransaction(final LogicalDatastoreType datastoreType) {
        requireNonNull(datastoreType, "datastoreType must not be null.");

        var entry = backingTx;
        if (entry == null) {
            if (!storeTxFactories.containsKey(datastoreType)) {
                throw new IllegalArgumentException(datastoreType + " is not supported");
            }
            final var tx = createTransaction(datastoreType);
            final var newEntry = Map.entry(datastoreType, tx);
            final var witness = (Entry<LogicalDatastoreType, T>) BACKING_TX.compareAndExchange(this, null, newEntry);
            if (witness != null) {
                tx.close();
                entry = witness;
            } else {
                entry = newEntry;
            }
        }

        final var expected = entry.getKey();
        if (expected != datastoreType) {
            throw new TransactionDatastoreMismatchException(expected, datastoreType);
        }
        return entry.getValue();
    }

    /**
     * Returns sub-transaction if initialized.
     */
    protected T getSubtransaction() {
        final Entry<LogicalDatastoreType, T> entry;
        return (entry = backingTx) == null ? null : entry.getValue();
    }

    protected abstract T createTransaction(LogicalDatastoreType datastoreType);

    @Override
    public Object getIdentifier() {
        return identifier;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    protected void closeSubtransaction() {
        if (backingTx != null) {
            try {
                backingTx.getValue().close();
            } catch (Exception e) {
                throw new IllegalStateException("Uncaught exception occurred during closing transaction", e);
            }
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
