/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract superclass of both ClientSnapshot and ClientTransaction. Provided for convenience.
 *
 * @author Robert Varga
 */
@Beta
public abstract class AbstractClientHandle<T extends AbstractProxyTransaction> extends LocalAbortable
        implements Identifiable<TransactionIdentifier> {
    /*
     * Our state consist of the the proxy map, hence we just subclass ConcurrentHashMap directly.
     */
    private static final class State<T> extends ConcurrentHashMap<Long, T> {
        private static final long serialVersionUID = 1L;
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractClientHandle.class);
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AbstractClientHandle, State> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(AbstractClientHandle.class, State.class, "state");

    private final TransactionIdentifier transactionId;
    private final AbstractClientHistory parent;

    private volatile State<T> state = new State<>();

    // Hidden to prevent outside instantiation
    AbstractClientHandle(final AbstractClientHistory parent, final TransactionIdentifier transactionId) {
        this.transactionId = requireNonNull(transactionId);
        this.parent = requireNonNull(parent);
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return transactionId;
    }

    /**
     * Release all state associated with this transaction.
     *
     * @return True if this transaction became closed during this call
     */
    public boolean abort() {
        if (commonAbort()) {
            parent.onTransactionAbort(this);
            return true;
        }

        return false;
    }

    private boolean commonAbort() {
        final Collection<T> toClose = ensureClosed();
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
    final @Nullable Collection<T> ensureClosed() {
        @SuppressWarnings("unchecked")
        final State<T> local = STATE_UPDATER.getAndSet(this, null);
        return local == null ? null : local.values();
    }

    final T ensureProxy(final YangInstanceIdentifier path) {
        final State<T> local = getState();
        final Long shard = parent.resolveShardForPath(path);

        return local.computeIfAbsent(shard, this::createProxy);
    }

    final AbstractClientHistory parent() {
        return parent;
    }

    abstract @NonNull T createProxy(@NonNull Long shard);

    private State<T> getState() {
        final State<T> local = state;
        checkState(local != null, "Transaction %s is closed", transactionId);
        return local;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues().add("identifier", transactionId).add("state", state)
                .toString();
    }
}
