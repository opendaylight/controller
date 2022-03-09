/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DOMBrokerTransactionChain extends AbstractDOMTransactionFactory<DOMStoreTransactionChain>
        implements DOMTransactionChain {
    private enum State {
        RUNNING,
        CLOSING,
        CLOSED,
        FAILED,
    }

    private static final AtomicIntegerFieldUpdater<DOMBrokerTransactionChain> COUNTER_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(DOMBrokerTransactionChain.class, "counter");
    private static final AtomicReferenceFieldUpdater<DOMBrokerTransactionChain, State> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(DOMBrokerTransactionChain.class, State.class, "state");
    private static final Logger LOG = LoggerFactory.getLogger(DOMBrokerTransactionChain.class);
    private final AtomicLong txNum = new AtomicLong();
    private final AbstractDOMBroker broker;
    private final DOMTransactionChainListener listener;
    private final long chainId;

    private volatile State state = State.RUNNING;
    private volatile int counter = 0;

    /**
     * Constructs an instance.
     *
     * @param chainId
     *            ID of transaction chain
     * @param chains
     *            Backing {@link DOMStoreTransactionChain}s.
     * @param listener
     *            Listener, which listens on transaction chain events.
     * @throws NullPointerException
     *             If any of arguments is null.
     */
    DOMBrokerTransactionChain(final long chainId, final Map<LogicalDatastoreType, DOMStoreTransactionChain> chains,
            final AbstractDOMBroker broker, final DOMTransactionChainListener listener) {
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
    public FluentFuture<? extends CommitInfo> commit(
            final DOMDataTreeWriteTransaction transaction, final Collection<DOMStoreThreePhaseCommitCohort> cohorts) {
        checkNotFailed();
        checkNotClosed();

        final FluentFuture<? extends CommitInfo> ret = broker.commit(transaction, cohorts);

        COUNTER_UPDATER.incrementAndGet(this);
        ret.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                transactionCompleted();
            }

            @Override
            public void onFailure(final Throwable failure) {
                transactionFailed(transaction, failure);
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

    private void transactionCompleted() {
        if (COUNTER_UPDATER.decrementAndGet(this) == 0 && state == State.CLOSING) {
            finishClose();
        }
    }

    private void transactionFailed(final DOMDataTreeWriteTransaction tx, final Throwable cause) {
        state = State.FAILED;
        LOG.debug("Transaction chain {} failed.", this, cause);
        listener.onTransactionChainFailed(this, tx, cause);
    }
}
