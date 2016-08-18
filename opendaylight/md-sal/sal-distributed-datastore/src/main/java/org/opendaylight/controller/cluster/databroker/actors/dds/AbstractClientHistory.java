/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
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
    private static final AtomicReferenceFieldUpdater<AbstractClientHistory, State> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(AbstractClientHistory.class, State.class, "state");

    private final Map<Long, LocalHistoryIdentifier> histories = new ConcurrentHashMap<>();
    private final DistributedDataStoreClientBehavior client;
    private final LocalHistoryIdentifier identifier;

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

    private LocalHistoryIdentifier getHistoryForCookie(final Long cookie) {
        LocalHistoryIdentifier ret = histories.get(cookie);
        if (ret == null) {
            ret = new LocalHistoryIdentifier(identifier.getClientId(), identifier.getHistoryId(), cookie);
            final LocalHistoryIdentifier existing = histories.putIfAbsent(cookie, ret);
            if (existing != null) {
                ret = existing;
            }
        }

        return ret;
    }

    @Override
    public final LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    final DistributedDataStoreClientBehavior getClient() {
        return client;
    }

    @Override
    final void localAbort(final Throwable cause) {
        LOG.debug("Force-closing history {}", getIdentifier(), cause);
        state = State.CLOSED;
    }

    final AbstractProxyTransaction createTransactionProxy(final TransactionIdentifier transactionId, final Long shard) {
        return AbstractProxyTransaction.create(client, getHistoryForCookie(shard),
            transactionId.getTransactionId(), client.resolver().getFutureBackendInfo(shard));
    }

    /**
     * Callback invoked from {@link ClientTransaction} when a transaction has been sub
     *
     * @param transaction Transaction handle
     */
    void onTransactionReady(final ClientTransaction transaction) {
        client.transactionComplete(transaction);
    }
}
