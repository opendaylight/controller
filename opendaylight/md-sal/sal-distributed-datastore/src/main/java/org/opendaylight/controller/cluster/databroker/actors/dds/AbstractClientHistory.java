/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.client.ConnectedClientConnection;
import org.opendaylight.controller.cluster.access.client.InversibleLockException;
import org.opendaylight.controller.cluster.access.commands.CreateLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.mdsal.common.api.TransactionChainClosedException;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for client view of a history. This class has two implementations, one for normal local histories
 * and the other for single transactions.
 *
 * @author Robert Varga
 */
abstract class AbstractClientHistory extends LocalAbortable implements Identifiable<LocalHistoryIdentifier> {
    enum State {
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

    private final Map<Long, ProxyHistory> histories = new ConcurrentHashMap<>();
    private final AbstractDataStoreClientBehavior client;
    private final LocalHistoryIdentifier identifier;

    // Used via NEXT_TX_UPDATER
    @SuppressWarnings("unused")
    private volatile long nextTx = 0;

    private volatile State state = State.IDLE;

    AbstractClientHistory(final AbstractDataStoreClientBehavior client, final LocalHistoryIdentifier identifier) {
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
        LOG.debug("Client history {} changed state from {} to {}", this, expected, next);
    }

    @Override
    public final LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    final long nextTx() {
        return NEXT_TX_UPDATER.getAndIncrement(this);
    }

    final Long resolveShardForPath(final YangInstanceIdentifier path) {
        return client.resolveShardForPath(path);
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
     * Create a new history proxy for a given shard.
     *
     * @throws InversibleLockException if the shard is being reconnected
     */
    private ProxyHistory createHistoryProxy(final Long shard) {
        final AbstractClientConnection<ShardBackendInfo> connection = client.getConnection(shard);
        final ProxyHistory ret = createHistoryProxy(new LocalHistoryIdentifier(identifier.getClientId(),
            identifier.getHistoryId(), shard), connection);

        // Request creation of the history, if it is not the single history
        if (ret.getIdentifier().getHistoryId() != 0) {
            connection.sendRequest(new CreateLocalHistoryRequest(ret.getIdentifier(), connection.localActor()),
                this::createHistoryCallback);
        }
        return ret;
    }

    abstract ProxyHistory createHistoryProxy(final LocalHistoryIdentifier historyId,
            final AbstractClientConnection<ShardBackendInfo> connection);

    private void createHistoryCallback(final Response<?, ?> response) {
        LOG.debug("Create history response {}", response);
    }

    final AbstractProxyTransaction createTransactionProxy(final TransactionIdentifier transactionId, final Long shard) {
        while (true) {
            final ProxyHistory history;
            try {
                history = histories.computeIfAbsent(shard, this::createHistoryProxy);
            } catch (InversibleLockException e) {
                LOG.trace("Waiting for transaction {} shard {} connection to resolve", transactionId, shard);
                e.awaitResolution();
                LOG.trace("Retrying transaction {} shard {} connection", transactionId, shard);
                continue;
            }

            return history.createTransactionProxy(transactionId);
        }
    }

    /**
     * Allocate a {@link ClientTransaction}.
     *
     * @return A new {@link ClientTransaction}
     * @throws TransactionChainClosedException if this history is closed
     */
    public final ClientTransaction createTransaction() {
        if (state == State.CLOSED) {
            throw new TransactionChainClosedException(String.format("Local history %s is closed", identifier));
        }

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

        LOG.debug("Local history {} readied transaction {}", this, txId);
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

    HistoryReconnectCohort startReconnect(final ConnectedClientConnection<ShardBackendInfo> newConn) {
        final ProxyHistory oldProxy = histories.get(newConn.cookie());
        if (oldProxy == null) {
            return null;
        }

        final ProxyReconnectCohort proxy = Verify.verifyNotNull(oldProxy.startReconnect(newConn));
        return new HistoryReconnectCohort() {
            @Override
            ProxyReconnectCohort getProxy() {
                return proxy;
            }

            @Override
            void replaySuccessfulRequests() {
                proxy.replaySuccessfulRequests();
            }

            @Override
            public void close() {
                LOG.debug("Client history {} finishing reconnect to {}", AbstractClientHistory.this, newConn);
                final ProxyHistory newProxy = proxy.finishReconnect();
                if (!histories.replace(newConn.cookie(), oldProxy, newProxy)) {
                    LOG.warn("Failed to replace proxy {} with {} in {}", oldProxy, newProxy,
                        AbstractClientHistory.this);
                }
            }
        };
    }
}
