/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for client view of a history. This class has two implementations, one for normal local histories
 * and the other for single transactions.
 *
 * @author Robert Varga
 */
abstract class AbstractClientHistory extends LocalAbortable implements Identifiable<LocalHistoryIdentifier> {
    static enum State {
        IDLE,
        TX_OPEN,
        CLOSED,
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractClientHistory.class);
    private static final AtomicLongFieldUpdater<AbstractClientHistory> NEXT_TX_UPDATER =
            AtomicLongFieldUpdater.newUpdater(AbstractClientHistory.class, "nextTx");
    private static final AtomicReferenceFieldUpdater<AbstractClientHistory, State> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(AbstractClientHistory.class, State.class, "state");

    @GuardedBy("this")
    private final Map<TransactionIdentifier, ClientTransaction> openTransactions = new HashMap<>();
    @GuardedBy("this")
    private final Map<TransactionIdentifier, AbstractTransactionCommitCohort> readyTransactions = new HashMap<>();

    private final Map<Long, AbstractProxyHistory> histories = new ConcurrentHashMap<>();
    private final DistributedDataStoreClientBehavior client;
    private final LocalHistoryIdentifier identifier;

    // Used via NEXT_TX_UPDATER
    @SuppressWarnings("unused")
    private volatile long nextTx = 0;

    private volatile State state = State.IDLE;

    AbstractClientHistory(final DistributedDataStoreClientBehavior client, final LocalHistoryIdentifier identifier) {
        this.client = Preconditions.checkNotNull(client);
        this.identifier = Preconditions.checkNotNull(identifier);
        Preconditions.checkArgument(identifier.getCookie() == 0);
    }

    final State state() {
        return state;
    }

    final void updateState(final State expected, final State next) {
        final boolean success = STATE_UPDATER.compareAndSet(this, expected, next);
        Preconditions.checkState(success, "Race condition detected, state changed from %s to %s", expected, state);
    }

    @Override
    public final LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    final DistributedDataStoreClientBehavior getClient() {
        return client;
    }

    final long nextTx() {
        return NEXT_TX_UPDATER.getAndIncrement(this);
    }

    @Override
    final void localAbort(final Throwable cause) {
        final State oldState = STATE_UPDATER.getAndSet(this, State.CLOSED);
        if (oldState != State.CLOSED) {
            LOG.debug("Force-closing history {}", getIdentifier(), cause);

            synchronized (this) {
                for (ClientTransaction t : openTransactions.values()) {
                    t.localAbort(cause);
                }
                openTransactions.clear();
                readyTransactions.clear();
            }
        }
    }

    /**
     *
     * @param shard
     * @return
     * @throws InversibleLockException
     */
    private AbstractProxyHistory createHistoryProxy(final Long shard) {
        final AbstractClientConnection connection = client.getConnection(shard);
        return createHistoryProxy(new LocalHistoryIdentifier(identifier.getClientId(),
            identifier.getHistoryId(), shard), connection);
    }

    abstract AbstractProxyHistory createHistoryProxy(final LocalHistoryIdentifier historyId,
            final AbstractClientConnection connection);

    /**
     *
     * @param transactionId
     * @param shard
     * @return
     * @throws InversibleLockException
     */
    final AbstractProxyTransaction createTransactionProxy(final TransactionIdentifier transactionId, final Long shard) {
        final AbstractProxyHistory history = histories.computeIfAbsent(shard, this::createHistoryProxy);
        return history.createTransactionProxy(transactionId);
    }

    public final ClientTransaction createTransaction() {
        Preconditions.checkState(state != State.CLOSED);

        synchronized (this) {
            final ClientTransaction ret = doCreateTransaction();
            openTransactions.put(ret.getIdentifier(), ret);
            return ret;
        }
    }

    @GuardedBy("this")
    abstract ClientTransaction doCreateTransaction();

    /**
     * Callback invoked from {@link ClientTransaction} when a child transaction readied for submission.
     *
     * @param txId Transaction identifier
     * @param cohort Transaction commit cohort
     */
    synchronized AbstractTransactionCommitCohort onTransactionReady(final TransactionIdentifier txId,
            final AbstractTransactionCommitCohort cohort) {
        final ClientTransaction tx = openTransactions.remove(txId);
        Preconditions.checkState(tx != null, "Failed to find open transaction for %s", txId);

        final AbstractTransactionCommitCohort previous = readyTransactions.putIfAbsent(txId, cohort);
        Preconditions.checkState(previous == null, "Duplicate cohort %s for transaction %s, already have %s",
                cohort, txId, previous);

        return cohort;
    }

    /**
     * Callback invoked from {@link ClientTransaction} when a child transaction has been aborted without touching
     * backend.
     *
     * @param txId transaction identifier
     */
    synchronized void onTransactionAbort(final TransactionIdentifier txId) {
        if (openTransactions.remove(txId) == null) {
            LOG.warn("Could not find aborting transaction {}", txId);
        }
    }

    /**
     * Callback invoked from {@link AbstractTransactionCommitCohort} when a child transaction has been completed
     * and all its state can be removed.
     *
     * @param txId transaction identifier
     */
    synchronized void onTransactionComplete(final TransactionIdentifier txId) {
        if (readyTransactions.remove(txId) == null) {
            LOG.warn("Could not find completed transaction {}", txId);
        }
    }

    synchronized void updateConnection(final Long shard, final ConnectedClientConnection newConn) {
        // TODO Auto-generated method stub

    }
}
