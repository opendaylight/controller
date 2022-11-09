/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.databroker;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FluentFuture;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.EnumMap;
import java.util.Map;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDOMTransactionFactory<T extends DOMStoreTransactionFactory> implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDOMTransactionFactory.class);
    private static final VarHandle CLOSED;

    static {
        try {
            CLOSED = MethodHandles.lookup().findVarHandle(AbstractDOMTransactionFactory.class, "closed", boolean.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Map<LogicalDatastoreType, T> storeTxFactories;

    private volatile boolean closed;

    protected AbstractDOMTransactionFactory(final Map<LogicalDatastoreType, T> txFactories) {
        this.storeTxFactories = new EnumMap<>(txFactories);
    }

    /**
     * Implementations must return unique identifier for each and every call of
     * this method.
     *
     * @return new Unique transaction identifier.
     */
    protected abstract Object newTransactionIdentifier();

    /**
     * Submits a transaction asynchronously for commit.
     *
     * @param transaction the transaction to submit
     * @param cohort the associated cohort
     * @return a resulting Future
     */
    protected abstract FluentFuture<? extends CommitInfo> commit(DOMDataTreeWriteTransaction transaction,
            DOMStoreThreePhaseCommitCohort cohort);

    /**
     * Creates a new read-only transaction.
     *
     * @return the transaction instance
     */
    public final DOMDataTreeReadTransaction newReadOnlyTransaction() {
        checkNotClosed();

        return new DOMBrokerReadOnlyTransaction(newTransactionIdentifier(), storeTxFactories);
    }


    /**
     * Creates a new write-only transaction.
     *
     * @return the transaction instance
     */
    public final DOMDataTreeWriteTransaction newWriteOnlyTransaction() {
        checkNotClosed();

        return new DOMBrokerWriteOnlyTransaction(newTransactionIdentifier(), storeTxFactories, this);
    }


    /**
     * Creates a new read-write transaction.
     *
     * @return the transaction instance
     */
    public final DOMDataTreeReadWriteTransaction newReadWriteTransaction() {
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
    public final Map<LogicalDatastoreType, T> getTxFactories() {
        return storeTxFactories;
    }

    /**
     * Checks if instance is not closed.
     *
     * @throws IllegalStateException If instance of this class was closed.
     *
     */
    protected final void checkNotClosed() {
        Preconditions.checkState(!closed, "Transaction factory was closed. No further operations allowed.");
    }

    @Override
    public void close() {
        if (!CLOSED.compareAndSet(this, false, true)) {
            LOG.warn("Transaction factory was already closed", new Throwable());
        }
    }
}
