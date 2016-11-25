/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Snapshot of the datastore state. Note this snapshot is not consistent across shards because sub-shard snapshots are
 * created lazily.
 *
 * @author Robert Varga
 */
@Beta
public class ClientSnapshot extends LocalAbortable implements Identifiable<TransactionIdentifier> {
    /*
     * Our state consist of the the proxy map, hence we just subclass ConcurrentHashMap directly.
     */
    private static final class State extends ConcurrentHashMap<Long, AbstractProxyTransaction> {
        private static final long serialVersionUID = 1L;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ClientSnapshot.class);
    private static final AtomicReferenceFieldUpdater<ClientSnapshot, State> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(ClientSnapshot.class, State.class, "state");

    private final TransactionIdentifier transactionId;
    private final AbstractClientHistory parent;

    private volatile State state = new State();

    ClientSnapshot(final AbstractClientHistory parent, final TransactionIdentifier transactionId) {
        this.transactionId = Preconditions.checkNotNull(transactionId);
        this.parent = Preconditions.checkNotNull(parent);
    }

    @Override
    public final TransactionIdentifier getIdentifier() {
        return transactionId;
    }

    public final CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {
        return ensureProxy(path).exists(path);
    }

    public final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
            final YangInstanceIdentifier path) {
        return ensureProxy(path).read(path);
    }

    /**
     * Release all state associated with this transaction.
     *
     * @return True if this transaction became closed during this call
     */
    public final boolean abort() {
        if (commonAbort()) {
            parent.onTransactionAbort(this);
            return true;
        }

        return false;
    }

    private boolean commonAbort() {
        final Collection<AbstractProxyTransaction> toClose = ensureClosed();
        if (toClose == null) {
            return false;
        }

        toClose.forEach(AbstractProxyTransaction::abort);
        return true;
    }

    @Override
    final void localAbort(final Throwable cause) {
        LOG.debug("Local abort of transaction {}", getIdentifier(), cause);
        commonAbort();
    }

    /**
     * Make sure this snapshot is closed. If it became closed as the effect of this call, return a collection of
     * {@link AbstractProxyTransaction} handles which need to be closed, too.
     *
     * @return null if this snapshot has already been closed, otherwise a collection of proxies, which need to be
     *         closed, too.
     */
    @Nullable final Collection<AbstractProxyTransaction> ensureClosed() {
        final State local = STATE_UPDATER.getAndSet(this, null);
        return local == null ? null : local.values();
    }

    final AbstractProxyTransaction ensureProxy(final YangInstanceIdentifier path) {
        final Map<Long, AbstractProxyTransaction> local = getState();
        final Long shard = parent.resolveShardForPath(path);

        return local.computeIfAbsent(shard, this::createProxy);
    }

    final AbstractClientHistory parent() {
        return parent;
    }

    private AbstractProxyTransaction createProxy(final Long shard) {
        return parent.createTransactionProxy(transactionId, shard);
    }

    private State getState() {
        final State local = state;
        Preconditions.checkState(local != null, "Transaction %s is closed", transactionId);
        return local;
    }
}
