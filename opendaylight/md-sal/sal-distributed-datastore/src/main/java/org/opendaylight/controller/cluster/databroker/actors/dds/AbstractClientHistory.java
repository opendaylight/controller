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
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
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

    private final Map<Long, LocalHistoryIdentifier> histories = new HashMap<>();
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

    final LocalHistoryIdentifier getHistoryForCookie(final Long cookie) {
        LocalHistoryIdentifier ret = histories.get(cookie);
        if (ret == null) {
            ret = new LocalHistoryIdentifier(identifier.getClientId(), identifier.getHistoryId(), cookie);
            histories.put(cookie, ret);
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


    /**
     * Callback invoked from {@link ClientTransaction} when a transaction has been completed, either successfully or
     * unsuccessfully, to remove all state tracking information.
     *
     * @param transaction Transaction handle
     */
    private void transactionComplete(final ClientTransaction transaction) {
        client.transactionComplete(transaction);
    }

    @Override
    final void localAbort(final Throwable cause) {
        LOG.debug("Force-closing history {}", getIdentifier(), cause);
        state = State.CLOSED;
    }

    void onTransactionSkipped(final ClientTransaction transaction) {
        transactionComplete(transaction);
    }

    void transactionCommitted(final ClientTransaction transaction) {
        final State local = state();
        Verify.verify(local == State.TX_OPEN, "Local history %s is in unexpected state %s", this, local);
        updateState(local, State.IDLE);
        transactionComplete(transaction);
    }
}
