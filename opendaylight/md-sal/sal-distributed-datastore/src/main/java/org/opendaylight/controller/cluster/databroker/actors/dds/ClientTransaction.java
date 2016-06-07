/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.access.commands.TransactionCanCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public final class ClientTransaction extends LocalAbortable implements Identifiable<TransactionIdentifier> {
    private static final ListenableFuture<Boolean> TRUE_FUTURE = Futures.immediateFuture(Boolean.TRUE);
    private static final ListenableFuture<Void> VOID_FUTURE = Futures.immediateFuture(null);

    private final class CommitCohort implements DOMStoreThreePhaseCommitCohort {
        private List<TransactionCanCommitSuccess> backends;

        @Override
        public ListenableFuture<Boolean> canCommit() {
            /*
             * For empty and single commits there is no action needed.
             *
             * Empty commits will only mark the fact they occured (for transaction ID tracking inside a local history).
             *
             * Single-entry commits will be committed in preCommit(), with any errors being reported in precommit phase.
             */
            if (proxies.size() <= 1) {
                return TRUE_FUTURE;
            }

            /*
             * Issue the request to commit for all participants. We will track the results and report them.
             */

            final List<ListenableFuture<TransactionSuccess<?>>> responses = new ArrayList<>(proxies.size());
            for (AbstractProxyTransaction proxy : proxies.values()) {
                responses.add(proxy.commit(true));
            }

            return Futures.transform(Futures.successfulAsList(responses),
                (AsyncFunction<List<TransactionSuccess<?>>, Boolean>)input -> {

                    final List<TransactionCanCommitSuccess> tmp = new ArrayList<>(input.size());
                    boolean ret = true;

                    int idx = 0;
                    for (TransactionSuccess<?> success : input) {
                        if (success instanceof TransactionCanCommitSuccess) {
                            tmp.add((TransactionCanCommitSuccess) success);
                            continue;
                        }

                        if (success == null) {
                            final ListenableFuture<TransactionSuccess<?>> f = responses.get(idx);
                            if (f.isDone()) {
                                try {
                                    f.get();
                                } catch (ExecutionException e) {
                                    LOG.debug("Transaction failed", e);
                                } catch (InterruptedException e) {
                                    // get() should never block, hence no interrupt
                                    Thread.currentThread().interrupt();
                                    LOG.error("Unexpected interrupt", e);
                                }
                            }
                        } else {
                            LOG.warn("Ignoring unexpected message {}", success);
                        }

                        ret = false;
                    }

                    backends = ImmutableList.copyOf(tmp);
                    return Futures.immediateFuture(ret);
                });
        }

        @Override
        public ListenableFuture<Void> preCommit() {
            // Just record the transaction id.
            if (proxies.isEmpty()) {
                // FIXME: upcall to ClientLocalHistory to record a skipped transaction.
                return VOID_FUTURE;
            }

            // Single backend, no need to coordinate, perform a direct commit
            if (proxies.size() == 1) {
                return Futures.transform(proxies.get(backends.get(0).getTarget()).commit(false),
                    (Function<TransactionSuccess<?>, Void>) input -> null);
            }

            // Coordinated commit: issue preCommit() messages
            for (TransactionCanCommitSuccess backend : backends) {
                // FIXME: we need to direct the response to the indicated actor, but sending to leader will work,
                //        because the backend can just forward it to its slave actor
                parent.getClient().sendRequest(null, (Consumer<Response<?, ?>>) null);
            }








            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ListenableFuture<Void> abort() {



            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ListenableFuture<Void> commit() {



            // TODO Auto-generated method stub
            return null;
        }

    }

    private static final Logger LOG = LoggerFactory.getLogger(ClientTransaction.class);
    private static final AtomicIntegerFieldUpdater<ClientTransaction> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(ClientTransaction.class, "state");
    private static final int OPEN_STATE = 0;
    private static final int CLOSED_STATE = 1;

    private final Map<Long, AbstractProxyTransaction> proxies = new HashMap<>();
    private final TransactionIdentifier transactionId;
    private final AbstractClientHistory parent;

    private volatile int state = OPEN_STATE;

    ClientTransaction(final DistributedDataStoreClientBehavior client, final AbstractClientHistory parent,
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
            Preconditions.checkState(success, "Transaction %s raced during close", this);
            return true;
        } else {
            return false;
        }
    }

    public DOMStoreThreePhaseCommitCohort ready() {
        Preconditions.checkState(ensureClosed(), "Attempted tu submit a closed transaction %s", this);

        for (AbstractProxyTransaction p : proxies.values()) {
            p.seal();
        }

        return new CommitCohort();
    }

    /**
     * Release all state associated with this transaction.
     */
    public void abort() {
        if (ensureClosed()) {
            for (AbstractProxyTransaction proxy : proxies.values()) {
                proxy.abort();
            }
            proxies.clear();
        }
    }

    @Override
    void localAbort(final Throwable cause) {
        LOG.debug("Aborting transaction {}", getIdentifier(), cause);
        abort();
    }
}
