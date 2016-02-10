/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.databroker;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;

public abstract class AbstractDOMTransactionFactory<T extends DOMStoreTransactionFactory> implements AutoCloseable {
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<AbstractDOMTransactionFactory> UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AbstractDOMTransactionFactory.class, "closed");
    private final Map<LogicalDatastoreType, T> storeTxFactories;
    private volatile int closed = 0;

    protected AbstractDOMTransactionFactory(final Map<LogicalDatastoreType, T> txFactories) {
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
     *
     * @param transaction
     * @param cohorts
     * @return
     */
    protected abstract CheckedFuture<Void,TransactionCommitFailedException> submit(final DOMDataWriteTransaction transaction,
                                                                                   final Collection<DOMStoreThreePhaseCommitCohort> cohorts);

    /**
     *
     * @return
     */
    public final DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        checkNotClosed();

        return new DOMBrokerReadOnlyTransaction(newTransactionIdentifier(), storeTxFactories);
    }


    /**
     *
     * @return
     */
    public final DOMDataWriteTransaction newWriteOnlyTransaction() {
        checkNotClosed();

        return new DOMBrokerWriteOnlyTransaction(newTransactionIdentifier(), storeTxFactories, this);
    }


    /**
     *
     * @return
     */
    public final DOMDataReadWriteTransaction newReadWriteTransaction() {
        checkNotClosed();

        return new DOMBrokerReadWriteTransaction(newTransactionIdentifier(), storeTxFactories, this);
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
