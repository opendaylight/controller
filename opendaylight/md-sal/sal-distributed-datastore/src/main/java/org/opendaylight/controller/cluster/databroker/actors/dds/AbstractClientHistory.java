/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.StampedLock;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.client.ConnectedClientConnection;
import org.opendaylight.controller.cluster.access.client.ConnectionEntry;
import org.opendaylight.controller.cluster.access.client.InversibleLockException;
import org.opendaylight.controller.cluster.access.commands.CreateLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainClosedException;
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
public abstract class AbstractClientHistory extends LocalAbortable implements Identifiable<LocalHistoryIdentifier> {
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
    private final Map<TransactionIdentifier, AbstractClientHandle<?>> openTransactions = new HashMap<>();
    @GuardedBy("this")
    private final Map<TransactionIdentifier, AbstractTransactionCommitCohort> readyTransactions = new HashMap<>();

    @GuardedBy("lock")
    private final Map<Long, ProxyHistory> histories = new ConcurrentHashMap<>();
    private final StampedLock lock = new StampedLock();

    /*
     *  FIXME: CONTROLLER-1896: track shard -> last purged transaction. The idea here is that we need to soft-track
     *                         when we have purged a transaction (across all shards) and cross-reference it with
     *                         outstanding transactions for a particular shard (i.e. openTransactions->state and
     *                         readyTransactions.
     *
     *                         The idea is that we need to keep an upper bound on the number of backlogged transactions
     *                         which each shard has not received a purged on in an efficient manner -- i.e. tell all
     *                         backends that transactions up to a specific identifier will not be seen in the future.
     *
     *                         Softness in tracking here means we do not keep a completely-uptodate view, but rather
     *                         allow for some configurable skew (say. 100 transactions) to be indeterminite, before
     *                         deciding to tell the backends (on individual basis?) about this at the moment we allocate
     *                         a new transaction towards that shard.
     *
     *                         This means that while under 'normal' conditions we do not need to issue these explicit
     *                         messages (because we have 1:1 shard/history mapping), we place an upper bound on how much
     *                         backend transaction tracking can fragment (in terms of RaneSet).
     *
     *                         Marking the transaction needs to be (reasonably) fast, as it needs to execute from when
     *                         the transaction is purged (either committed, aborted or GC'd) and the actual propagation
     *                         may take a a special-case path from createSnapshot()/createTransaction(). Those look like
     *                         they'll end up doing more than just returning a snapshot.
     *
     *                         Note: the above needs to be reconciled with actual request lifecycle.
     */

    private final AbstractDataStoreClientBehavior client;
    private final LocalHistoryIdentifier identifier;

    // Used via NEXT_TX_UPDATER
    @SuppressWarnings("unused")
    private volatile long nextTx = 0;

    private volatile State state = State.IDLE;

    AbstractClientHistory(final AbstractDataStoreClientBehavior client, final LocalHistoryIdentifier identifier) {
        this.client = requireNonNull(client);
        this.identifier = requireNonNull(identifier);
        checkArgument(identifier.getCookie() == 0);
    }

    final State state() {
        return state;
    }

    final void updateState(final State expected, final State next) {
        final boolean success = STATE_UPDATER.compareAndSet(this, expected, next);
        checkState(success, "Race condition detected, state changed from %s to %s", expected, state);
        LOG.debug("Client history {} changed state from {} to {}", this, expected, next);
    }

    final synchronized void doClose() {
        final State local = state;
        if (local != State.CLOSED) {
            checkState(local == State.IDLE, "Local history %s has an open transaction", this);
            histories.values().forEach(ProxyHistory::close);
            updateState(local, State.CLOSED);
        }
    }

    final synchronized void onProxyDestroyed(final ProxyHistory proxyHistory) {
        histories.remove(proxyHistory.getIdentifier().getCookie());
        LOG.debug("{}: removed destroyed proxy {}", this, proxyHistory);
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
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
                for (AbstractClientHandle<?> t : openTransactions.values()) {
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
    @GuardedBy("lock")
    private ProxyHistory createHistoryProxy(final Long shard) {
        final AbstractClientConnection<ShardBackendInfo> connection = client.getConnection(shard);
        final LocalHistoryIdentifier proxyId = new LocalHistoryIdentifier(identifier.getClientId(),
            identifier.getHistoryId(), shard);
        LOG.debug("Created proxyId {} for history {} shard {}", proxyId, identifier, shard);

        final ProxyHistory ret = createHistoryProxy(proxyId, connection);

        // Request creation of the history, if it is not the single history
        if (ret.getIdentifier().getHistoryId() != 0) {
            connection.sendRequest(new CreateLocalHistoryRequest(ret.getIdentifier(), connection.localActor()),
                this::createHistoryCallback);
        }
        return ret;
    }

    abstract ProxyHistory createHistoryProxy(LocalHistoryIdentifier historyId,
            AbstractClientConnection<ShardBackendInfo> connection);

    private void createHistoryCallback(final Response<?, ?> response) {
        LOG.debug("Create history response {}", response);
    }

    private ProxyHistory ensureHistoryProxy(final TransactionIdentifier transactionId, final Long shard) {
        while (true) {
            try {
                // Short-lived lock to ensure exclusion of createHistoryProxy and the lookup phase in startReconnect,
                // see comments in startReconnect() for details.
                final long stamp = lock.readLock();
                try {
                    return histories.computeIfAbsent(shard, this::createHistoryProxy);
                } finally {
                    lock.unlockRead(stamp);
                }
            } catch (InversibleLockException e) {
                LOG.trace("Waiting for transaction {} shard {} connection to resolve", transactionId, shard);
                e.awaitResolution();
                LOG.trace("Retrying transaction {} shard {} connection", transactionId, shard);
            }
        }
    }

    final AbstractProxyTransaction createSnapshotProxy(final TransactionIdentifier transactionId, final Long shard) {
        return ensureHistoryProxy(transactionId, shard).createTransactionProxy(transactionId, true);
    }

    final AbstractProxyTransaction createTransactionProxy(final TransactionIdentifier transactionId, final Long shard) {
        return ensureHistoryProxy(transactionId, shard).createTransactionProxy(transactionId, false);
    }

    private void checkNotClosed() {
        if (state == State.CLOSED) {
            throw new DOMTransactionChainClosedException(String.format("Local history %s is closed", identifier));
        }
    }

    /**
     * Allocate a new {@link ClientTransaction}.
     *
     * @return A new {@link ClientTransaction}
     * @throws DOMTransactionChainClosedException if this history is closed
     * @throws IllegalStateException if a previous dependent transaction has not been closed
     */
    public ClientTransaction createTransaction() {
        checkNotClosed();

        synchronized (this) {
            final ClientTransaction ret = doCreateTransaction();
            openTransactions.put(ret.getIdentifier(), ret);
            return ret;
        }
    }

    /**
     * Create a new {@link ClientSnapshot}.
     *
     * @return A new {@link ClientSnapshot}
     * @throws DOMTransactionChainClosedException if this history is closed
     * @throws IllegalStateException if a previous dependent transaction has not been closed
     */
    public ClientSnapshot takeSnapshot() {
        checkNotClosed();

        synchronized (this) {
            final ClientSnapshot ret = doCreateSnapshot();
            openTransactions.put(ret.getIdentifier(), ret);
            return ret;
        }
    }

    @GuardedBy("this")
    abstract ClientSnapshot doCreateSnapshot();

    @GuardedBy("this")
    abstract ClientTransaction doCreateTransaction();

    /**
     * Callback invoked from {@link ClientTransaction} when a child transaction readied for submission.
     *
     * @param txId Transaction identifier
     * @param cohort Transaction commit cohort
     */
    synchronized AbstractTransactionCommitCohort onTransactionReady(final ClientTransaction tx,
            final AbstractTransactionCommitCohort cohort) {
        final TransactionIdentifier txId = tx.getIdentifier();
        if (openTransactions.remove(txId) == null) {
            LOG.warn("Transaction {} not recorded, proceeding with readiness", txId);
        }

        final AbstractTransactionCommitCohort previous = readyTransactions.putIfAbsent(txId, cohort);
        checkState(previous == null, "Duplicate cohort %s for transaction %s, already have %s", cohort, txId, previous);

        LOG.debug("Local history {} readied transaction {}", this, txId);
        return cohort;
    }

    /**
     * Callback invoked from {@link ClientTransaction} when a child transaction has been aborted without touching
     * backend.
     *
     * @param snapshot transaction identifier
     */
    synchronized void onTransactionAbort(final AbstractClientHandle<?> snapshot) {
        if (openTransactions.remove(snapshot.getIdentifier()) == null) {
            LOG.warn("Could not find aborting transaction {}", snapshot.getIdentifier());
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
        /*
         * This looks ugly and unusual and there is a reason for that, as the locking involved is in multiple places.
         *
         * We need to make sure that a new proxy is not created while we are reconnecting, which is partially satisfied
         * by client.getConnection() throwing InversibleLockException by the time this method is invoked. That does
         * not cover the case when createHistoryProxy() has already acquired the connection, but has not yet populated
         * the history map.
         *
         * Hence we need to make sure no potential computation is happening concurrently with us looking at the history
         * map. Once we have performed that lookup, though, we can release the lock immediately, as all creation
         * requests are established to happen either before or after the reconnect attempt.
         */
        final ProxyHistory oldProxy;
        final long stamp = lock.writeLock();
        try {
            oldProxy = histories.get(newConn.cookie());
        } finally {
            lock.unlockWrite(stamp);
        }

        if (oldProxy == null) {
            return null;
        }

        final ProxyReconnectCohort proxy = verifyNotNull(oldProxy.startReconnect(newConn));
        return new HistoryReconnectCohort() {
            @Override
            ProxyReconnectCohort getProxy() {
                return proxy;
            }

            @Override
            void replayRequests(final Collection<ConnectionEntry> previousEntries) {
                proxy.replayRequests(previousEntries);
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
