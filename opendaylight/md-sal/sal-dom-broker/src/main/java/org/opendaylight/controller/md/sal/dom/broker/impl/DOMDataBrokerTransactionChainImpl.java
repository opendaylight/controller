/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Supplier;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NormalizedNode implementation of {@link org.opendaylight.controller.md.sal.common.api.data.TransactionChain} which
 * is backed
 * by several {@link DOMStoreTransactionChain} differentiated by provided
 * {@link org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType} type.
 */
@Deprecated
final class DOMDataBrokerTransactionChainImpl extends
        AbstractDOMForwardedTransactionFactory<DOMStoreTransactionChain> implements DOMTransactionChain {
    private enum State {
        RUNNING, CLOSING, CLOSED, FAILED,
    }

    private static final AtomicIntegerFieldUpdater<DOMDataBrokerTransactionChainImpl> COUNTER_UPDATER
            = AtomicIntegerFieldUpdater.newUpdater(DOMDataBrokerTransactionChainImpl.class, "counter");
    private static final AtomicReferenceFieldUpdater<DOMDataBrokerTransactionChainImpl, State> STATE_UPDATER
            = AtomicReferenceFieldUpdater.newUpdater(DOMDataBrokerTransactionChainImpl.class, State.class, "state");
    private static final Logger LOG = LoggerFactory.getLogger(DOMDataBrokerTransactionChainImpl.class);
    private final AtomicLong txNum = new AtomicLong();
    private final AbstractDOMDataBroker broker;
    private final TransactionChainListener listener;
    private final long chainId;

    private volatile State state = State.RUNNING;
    private volatile int counter = 0;

    /**
     * DOMDataBrokerTransactionChainImpl constructor.
     *
     * @param chainId  ID of transaction chain
     * @param chains   Backing {@link DOMStoreTransactionChain}s.
     * @param broker   Commit Coordinator which should be used to coordinate commits
     *                 of transaction
     *                 produced by this chain.
     * @param listener Listener, which listens on transaction chain events.
     * @throws NullPointerException If any of arguments is null.
     */
    DOMDataBrokerTransactionChainImpl(final long chainId,
                                             final Map<LogicalDatastoreType, DOMStoreTransactionChain> chains,
                                             final AbstractDOMDataBroker broker,
                                             final TransactionChainListener listener) {
        super(chains);
        this.chainId = chainId;
        this.broker = requireNonNull(broker);
        this.listener = requireNonNull(listener);
    }

    private void checkNotFailed() {
        checkState(state != State.FAILED, "Transaction chain has failed");
    }

    @Override
    protected Object newTransactionIdentifier() {
        return "DOM-CHAIN-" + chainId + "-" + txNum.getAndIncrement();
    }

    @Override
    public <T> ListenableFuture<T> commit(final DOMDataWriteTransaction transaction,
            final Collection<DOMStoreThreePhaseCommitCohort> cohorts, final Supplier<T> futureValueSupplier) {
        checkNotFailed();
        checkNotClosed();

        final ListenableFuture<T> ret = broker.commit(transaction, cohorts, futureValueSupplier);

        COUNTER_UPDATER.incrementAndGet(this);
        Futures.addCallback(ret, new FutureCallback<T>() {
            @Override
            public void onSuccess(final T result) {
                transactionCompleted();
            }

            @Override
            public void onFailure(final Throwable throwable) {
                transactionFailed(transaction, throwable);
            }
        }, MoreExecutors.directExecutor());

        return ret;
    }

    @Override
    public void close() {
        final boolean success = STATE_UPDATER.compareAndSet(this, State.RUNNING, State.CLOSING);
        if (!success) {
            LOG.debug("Chain {} is no longer running", this);
            return;
        }

        super.close();
        for (DOMStoreTransactionChain subChain : getTxFactories().values()) {
            subChain.close();
        }

        if (counter == 0) {
            finishClose();
        }
    }

    private void finishClose() {
        state = State.CLOSED;
        listener.onTransactionChainSuccessful(this);
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private void transactionCompleted() {
        if (COUNTER_UPDATER.decrementAndGet(this) == 0 && state == State.CLOSING) {
            finishClose();
        }
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private void transactionFailed(final DOMDataWriteTransaction tx, final Throwable cause) {
        state = State.FAILED;
        LOG.debug("Transaction chain {} failed.", this, cause);
        listener.onTransactionChainFailed(this, tx, cause);
    }
}
