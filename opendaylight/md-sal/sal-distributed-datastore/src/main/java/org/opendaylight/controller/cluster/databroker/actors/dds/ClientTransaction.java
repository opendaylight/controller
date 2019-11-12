/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Client-side view of a transaction.
 *
 * <p>
 * This interface is used by the world outside of the actor system and in the actor system it is manifested via
 * its client actor. That requires some state transfer with {@link DistributedDataStoreClientBehavior}. In order to
 * reduce request latency, all messages are carbon-copied (and enqueued first) to the client actor.
 *
 * <p>
 * It is internally composed of multiple {@link RemoteProxyTransaction}s, each responsible for a component shard.
 *
 * <p>
 * Implementation is quite a bit complex, and involves cooperation with {@link AbstractClientHistory} for tracking
 * gaps in transaction identifiers seen by backends.
 *
 * <p>
 * These gaps need to be accounted for in the transaction setup message sent to a particular backend, so it can verify
 * that the requested transaction is in-sequence. This is critical in ensuring that transactions (which are independent
 * entities from message queueing perspective) do not get reodered -- thus allowing multiple in-flight transactions.
 *
 * <p>
 * Alternative would be to force visibility by sending an abort request to all potential backends, but that would mean
 * that even empty transactions increase load on all shards -- which would be a scalability issue.
 *
 * <p>
 * Yet another alternative would be to introduce inter-transaction dependencies to the queueing layer in client actor,
 * but that would require additional indirection and complexity.
 *
 * @author Robert Varga
 */
@Beta
public class ClientTransaction extends AbstractClientHandle<AbstractProxyTransaction> {
    ClientTransaction(final AbstractClientHistory parent, final TransactionIdentifier transactionId) {
        super(parent, transactionId);
    }

    private AbstractProxyTransaction ensureTransactionProxy(final YangInstanceIdentifier path) {
        return ensureProxy(path);
    }

    public FluentFuture<Boolean> exists(final YangInstanceIdentifier path) {
        return ensureTransactionProxy(path).exists(path);
    }

    public FluentFuture<Optional<NormalizedNode>> read(final YangInstanceIdentifier path) {
        return ensureTransactionProxy(path).read(path);
    }

    public void delete(final YangInstanceIdentifier path) {
        ensureTransactionProxy(path).delete(path);
    }

    public void merge(final YangInstanceIdentifier path, final NormalizedNode data) {
        ensureTransactionProxy(path).merge(path, data);
    }

    public void write(final YangInstanceIdentifier path, final NormalizedNode data) {
        ensureTransactionProxy(path).write(path, data);
    }

    public DOMStoreThreePhaseCommitCohort ready() {
        final Map<Long, AbstractProxyTransaction> participants = ensureClosed();
        checkState(participants != null, "Attempted to submit a closed transaction %s", this);

        final TransactionIdentifier txId = getIdentifier();
        final AbstractClientHistory parent = parent();
        parent.onTransactionShardsBound(txId, participants.keySet());

        final Collection<AbstractProxyTransaction> toReady = participants.values();
        toReady.forEach(AbstractProxyTransaction::seal);

        final AbstractTransactionCommitCohort cohort;
        switch (toReady.size()) {
            case 0:
                cohort = new EmptyTransactionCommitCohort(parent, txId);
                break;
            case 1:
                cohort = new DirectTransactionCommitCohort(parent, txId, toReady.iterator().next());
                break;
            default:
                cohort = new ClientTransactionCommitCohort(parent, txId, toReady);
                break;
        }

        return parent.onTransactionReady(this, cohort);
    }

    @Override
    final AbstractProxyTransaction createProxy(final Long shard) {
        return parent().createTransactionProxy(getIdentifier(), shard);
    }
}
