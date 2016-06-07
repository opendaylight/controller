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
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Client-side view of a free-standing transaction.
 *
 * This interface is used by the world outside of the actor system and in the actor system it is manifested via
 * its client actor. That requires some state transfer with {@link DistributedDataStoreClientBehavior}. In order to
 * reduce request latency, all messages are carbon-copied (and enqueued first) to the client actor.
 *
 * It is internally composed of multiple {@link RemoteProxyTransaction}s, each responsible for a component shard.
 *
 * @author Robert Varga
 */
@Beta
public final class ClientSingleTransaction implements AutoCloseable, Identifiable<TransactionIdentifier> {
    private static final AtomicIntegerFieldUpdater<ClientSingleTransaction> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(ClientSingleTransaction.class, "state");
    private static final int OPEN_STATE = 0;
    private static final int CLOSED_STATE = 1;

    private final Map<Long, AbstractProxyTransaction> proxies = new HashMap<>();
    private final TransactionIdentifier transactionId;
    private final AbstractClientHistory parent;

    private volatile int state = OPEN_STATE;

    ClientSingleTransaction(final DistributedDataStoreClientBehavior client, final AbstractClientHistory parent,
        final TransactionIdentifier transactionId) {
        this.transactionId = Preconditions.checkNotNull(transactionId);
        this.parent = Preconditions.checkNotNull(parent);
    }

    private void checkNotClosed() {
        Preconditions.checkState(state == OPEN_STATE, "Transaction %s is closed", transactionId);
    }

    private AbstractProxyTransaction ensureProxy(final Long shard) {
        checkNotClosed();

        AbstractProxyTransaction ret = proxies.get(shard);
        if (ret == null) {
            // FIXME: we need ShardBackedInfo if available
            ret = AbstractProxyTransaction.create(parent.getClient(), parent.getHistoryForCookie(shard),
                transactionId.getTransactionId(), null);
            proxies.put(shard, ret);
        }
        return ret;
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return transactionId;
    }

    public CheckedFuture<Boolean, ReadFailedException> exists(final Long shard, final YangInstanceIdentifier path) {
        return ensureProxy(shard).exists(path);
    }

    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final Long shard,
            final YangInstanceIdentifier path) {
        return ensureProxy(shard).read(path);
    }

    public void delete(final Long shard, final YangInstanceIdentifier path) {
        ensureProxy(shard).delete(path);
    }

    public void merge(final Long shard, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        ensureProxy(shard).merge(path, data);
    }

    public void write(final Long shard, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        ensureProxy(shard).write(path, data);
    }

    private boolean ensureClosed() {
        final int local = state;
        if (local != CLOSED_STATE) {
            final boolean success = STATE_UPDATER.compareAndSet(this, OPEN_STATE, CLOSED_STATE);
            Preconditions.checkState(success, "Transaction %s cased during close", this);
            return true;
        } else {
            return false;
        }
    }

    private DOMStoreThreePhaseCommitCohort readySingle() {
        final AbstractProxyTransaction p = Iterables.getOnlyElement(proxies.values());
        proxies.clear();
        return null;
    }

    private DOMStoreThreePhaseCommitCohort readyMulti() {
        // FIXME: Submit all in coordinated fashion
        return null;
    }

    public DOMStoreThreePhaseCommitCohort ready() {
        Preconditions.checkState(ensureClosed(), "Attempted tu submit a closed transaction %s", this);

        for (AbstractProxyTransaction p : proxies.values()) {
            p.seal();
        }

        switch (proxies.size()) {
            case 0:
                // No-op
                return NoOpDOMStoreThreePhaseCommitCohort.INSTANCE;
            case 1:
                return readySingle();
            default:
                return readyMulti();
        }
    }

    @Override
    public void close() {
        if (ensureClosed()) {
            for (AbstractProxyTransaction proxy : proxies.values()) {
                proxy.abort();
            }
            proxies.clear();
        }
    }

}
