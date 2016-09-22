/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

/**
 *
 * Abstract composite transaction factory.
 *
 * Provides an convenience common implementation for composite DOM Transactions,
 * where subtransaction is identified by {@link LogicalDatastoreType} type and
 * implementation of subtransaction is provided by
 * {@link DOMStoreTransactionFactory}.
 *
 * <b>Note:</b>This class does not have thread-safe implementation of  {@link #close()},
 *   implementation may allow accessing and allocating new transactions during closing
 *   this instance.
 *
 * @param <T>
 *            Type of {@link DOMStoreTransactionFactory} factory.
 */
abstract class AbstractDOMForwardedTransactionFactory<T extends DOMStoreTransactionFactory> implements AutoCloseable {
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<AbstractDOMForwardedTransactionFactory> UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AbstractDOMForwardedTransactionFactory.class, "closed");
    private final Map<LogicalDatastoreType, T> storeTxFactories;
    private volatile int closed = 0;

    protected AbstractDOMForwardedTransactionFactory(final Map<LogicalDatastoreType, ? extends T> txFactories) {
        this.storeTxFactories = new EnumMap<>(txFactories);
    }

    /**
     * Implementations must return unique identifier for each and every call of
     * this method;
     *
     * @return new Unique transaction identifier.
     */
    protected abstract Object newTransactionIdentifier();

    /**
     * User-supplied implementation of {@link DOMDataWriteTransaction#submit()}
     * for transaction.
     *
     * Callback invoked when {@link DOMDataWriteTransaction#submit()} is invoked
     * on transaction created by this factory.
     *
     * @param transaction
     *            Transaction on which {@link DOMDataWriteTransaction#commit()}
     *            was invoked.
     * @param cohorts
     *            Iteratable of cohorts for subtransactions associated with
     *            the transaction being committed.
     * @return a CheckedFuture. if commit coordination on cohorts finished successfully,
     *         nothing is returned from the Future, On failure,
     *         the Future fails with a {@link TransactionCommitFailedException}.
     */
    protected abstract CheckedFuture<Void,TransactionCommitFailedException> submit(final DOMDataWriteTransaction transaction,
            final Collection<DOMStoreThreePhaseCommitCohort> cohorts);

    /**
     * Creates a new composite read-only transaction
     *
     * Creates a new composite read-only transaction backed by one transaction
     * per factory in {@link #getTxFactories()}.
     *
     * Subtransaction for reading is selected by supplied
     * {@link LogicalDatastoreType} as parameter for
     * {@link DOMDataReadOnlyTransaction#read(LogicalDatastoreType,org.opendaylight.yangtools.yang.data.api.InstanceIdentifier)}
     * .
     *
     * Id of returned transaction is retrieved via
     * {@link #newTransactionIdentifier()}.
     *
     * @return New composite read-only transaction.
     */
    public final DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        checkNotClosed();

        final Map<LogicalDatastoreType, DOMStoreReadTransaction> txns = new EnumMap<>(LogicalDatastoreType.class);
        for (Entry<LogicalDatastoreType, T> store : storeTxFactories.entrySet()) {
            txns.put(store.getKey(), store.getValue().newReadOnlyTransaction());
        }
        return new DOMForwardedReadOnlyTransaction(newTransactionIdentifier(), txns);
    }

    /**
     * Creates a new composite write-only transaction
     *
     * <p>
     * Creates a new composite write-only transaction backed by one write-only
     * transaction per factory in {@link #getTxFactories()}.
     *
     * <p>
     * Implementation of composite Write-only transaction is following:
     *
     * <ul>
     * <li>
     * {@link DOMDataWriteTransaction#put(LogicalDatastoreType, org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode)}
     * - backing subtransaction is selected by {@link LogicalDatastoreType},
     * {@link DOMStoreWriteTransaction#write(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode)}
     * is invoked on selected subtransaction.
     * <li>
     * {@link DOMDataWriteTransaction#merge(LogicalDatastoreType, org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode)}
     * - backing subtransaction is selected by {@link LogicalDatastoreType},
     * {@link DOMStoreWriteTransaction#merge(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode)}
     * is invoked on selected subtransaction.
     * <li>
     * {@link DOMDataWriteTransaction#delete(LogicalDatastoreType, org.opendaylight.yangtools.yang.data.api.InstanceIdentifier)
     * - backing subtransaction is selected by {@link LogicalDatastoreType},
     * {@link DOMStoreWriteTransaction#delete(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier)} is invoked on
     * selected subtransaction.
     * <li> {@link DOMDataWriteTransaction#commit()} - results in invoking
     * {@link DOMStoreWriteTransaction#ready()}, gathering all resulting cohorts
     * and then invoking finalized implementation callback
     * {@link #submit(DOMDataWriteTransaction, Iterable)} with transaction which
     * was commited and gathered results.
     * </ul>
     *
     * Id of returned transaction is generated via
     * {@link #newTransactionIdentifier()}.
     *
     * @return New composite write-only transaction associated with this
     *         factory.
     */
    public final DOMDataWriteTransaction newWriteOnlyTransaction() {
        checkNotClosed();

        final Map<LogicalDatastoreType, DOMStoreWriteTransaction> txns = new EnumMap<>(LogicalDatastoreType.class);
        for (Entry<LogicalDatastoreType, T> store : storeTxFactories.entrySet()) {
            txns.put(store.getKey(), store.getValue().newWriteOnlyTransaction());
        }
        return new DOMForwardedWriteTransaction<>(newTransactionIdentifier(), txns, this);
    }

    /**
     * Creates a new composite write-only transaction
     *
     * <p>
     * Creates a new composite write-only transaction backed by one write-only
     * transaction per factory in {@link #getTxFactories()}.
     * <p>
     * Implementation of composite Write-only transaction is following:
     *
     * <ul>
     * <li>
     * {@link DOMDataWriteTransaction#read(LogicalDatastoreType, org.opendaylight.yangtools.yang.data.api.InstanceIdentifier)}
     * - backing subtransaction is selected by {@link LogicalDatastoreType},
     * {@link DOMStoreWriteTransaction#read(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier)} is invoked on
     * selected subtransaction.
     * <li>
     * {@link DOMDataWriteTransaction#put(LogicalDatastoreType, org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode)}
     * - backing subtransaction is selected by {@link LogicalDatastoreType},
     * {@link DOMStoreWriteTransaction#write(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode)}
     * is invoked on selected subtransaction.
     * <li>
     * {@link DOMDataWriteTransaction#merge(LogicalDatastoreType, org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode)}
     * - backing subtransaction is selected by {@link LogicalDatastoreType},
     * {@link DOMStoreWriteTransaction#merge(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode)}
     * is invoked on selected subtransaction.
     * <li>
     * {@link DOMDataWriteTransaction#delete(LogicalDatastoreType, org.opendaylight.yangtools.yang.data.api.InstanceIdentifier)
     * - backing subtransaction is selected by {@link LogicalDatastoreType},
     * {@link DOMStoreWriteTransaction#delete(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier)} is invoked on
     * selected subtransaction.
     * <li> {@link DOMDataWriteTransaction#commit()} - results in invoking
     * {@link DOMStoreWriteTransaction#ready()}, gathering all resulting cohorts
     * and then invoking finalized implementation callback
     * {@link #submit(DOMDataWriteTransaction, Iterable)} with transaction which
     * was commited and gathered results.
     * <li>
     * </ul>
     *
     * Id of returned transaction is generated via
     * {@link #newTransactionIdentifier()}.
     *
     * @return New composite read-write transaction associated with this
     *         factory.
     */
    public final DOMDataReadWriteTransaction newReadWriteTransaction() {
        checkNotClosed();

        final Map<LogicalDatastoreType, DOMStoreReadWriteTransaction> txns = new EnumMap<>(LogicalDatastoreType.class);
        for (Entry<LogicalDatastoreType, T> store : storeTxFactories.entrySet()) {
            txns.put(store.getKey(), store.getValue().newReadWriteTransaction());
        }
        return new DOMForwardedReadWriteTransaction(newTransactionIdentifier(), txns, this);
    }

    /**
     * Convenience accessor of backing factories intended to be used only by
     * finalization of this class.
     *
     * <b>Note:</b>
     * Finalization of this class may want to access other functionality of
     * supplied Transaction factories.
     *
     * @return Map of backing transaction factories.
     */
    protected final Map<LogicalDatastoreType, T> getTxFactories() {
        return storeTxFactories;
    }

    /**
     * Checks if instance is not closed.
     *
     * @throws IllegalStateException If instance of this class was closed.
     *
     */
    protected final void checkNotClosed() {
        Preconditions.checkState(closed == 0, "Transaction factory was closed. No further operations allowed.");
    }

    @Override
    public void close() {
        final boolean success = UPDATER.compareAndSet(this, 0, 1);
        Preconditions.checkState(success, "Transaction factory was already closed");
    }
}

