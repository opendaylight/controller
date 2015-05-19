/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.NormalizedNodeAggregator;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.AbstractDOMStoreTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.util.concurrent.MappingCheckedFuture;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * A transaction potentially spanning multiple backend shards.
 */
public class TransactionProxy extends AbstractDOMStoreTransaction<TransactionIdentifier> implements DOMStoreReadWriteTransaction {
    private static enum TransactionState {
        OPEN,
        READY,
        CLOSED,
    }
    private static final Logger LOG = LoggerFactory.getLogger(TransactionProxy.class);

    private final Map<String, TransactionContextWrapper> txContextAdapters = new HashMap<>();
    private final AbstractTransactionContextFactory<?> txContextFactory;
    private final OperationLimiter limiter;
    private final TransactionType type;
    private TransactionState state = TransactionState.OPEN;

    @VisibleForTesting
    public TransactionProxy(final AbstractTransactionContextFactory<?> txContextFactory, final TransactionType type) {
        super(txContextFactory.nextIdentifier(), false);
        this.txContextFactory = txContextFactory;
        this.type = Preconditions.checkNotNull(type);

        // Note : Currently mailbox-capacity comes from akka.conf and not from the config-subsystem
        this.limiter = new OperationLimiter(getIdentifier(),
            getActorContext().getTransactionOutstandingOperationLimit(),
            getActorContext().getDatastoreContext().getOperationTimeoutInSeconds());

        LOG.debug("New {} Tx - {}", type, getIdentifier());
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {
        Preconditions.checkState(type != TransactionType.WRITE_ONLY, "Reads from write-only transactions are not allowed");

        LOG.debug("Tx {} exists {}", getIdentifier(), path);

        limiter.throttleOperation();

        final SettableFuture<Boolean> proxyFuture = SettableFuture.create();
        TransactionContextWrapper contextAdapter = getContextAdapter(path);
        contextAdapter.maybeExecuteTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.dataExists(path, proxyFuture);
            }
        });

        return MappingCheckedFuture.create(proxyFuture, ReadFailedException.MAPPER);
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final YangInstanceIdentifier path) {
        Preconditions.checkState(type != TransactionType.WRITE_ONLY, "Reads from write-only transactions are not allowed");

        LOG.debug("Tx {} read {}", getIdentifier(), path);

        if (YangInstanceIdentifier.EMPTY.equals(path)) {
            return readAllData();
        } else {
            limiter.throttleOperation();

            return singleShardRead(shardNameFromIdentifier(path), path);
        }
    }

    private CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> singleShardRead(
            final String shardName, final YangInstanceIdentifier path) {
        final SettableFuture<Optional<NormalizedNode<?, ?>>> proxyFuture = SettableFuture.create();
        TransactionContextWrapper contextAdapter = getContextAdapter(shardName);
        contextAdapter.maybeExecuteTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.readData(path, proxyFuture);
            }
        });

        return MappingCheckedFuture.create(proxyFuture, ReadFailedException.MAPPER);
    }

    private CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readAllData() {
        final Set<String> allShardNames = txContextFactory.getActorContext().getConfiguration().getAllShardNames();
        final Collection<CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException>> futures = new ArrayList<>(allShardNames.size());

        for (String shardName : allShardNames) {
            futures.add(singleShardRead(shardName, YangInstanceIdentifier.EMPTY));
        }

        final ListenableFuture<List<Optional<NormalizedNode<?, ?>>>> listFuture = Futures.allAsList(futures);
        final ListenableFuture<Optional<NormalizedNode<?, ?>>> aggregateFuture;

        aggregateFuture = Futures.transform(listFuture, new Function<List<Optional<NormalizedNode<?, ?>>>, Optional<NormalizedNode<?, ?>>>() {
            @Override
            public Optional<NormalizedNode<?, ?>> apply(final List<Optional<NormalizedNode<?, ?>>> input) {
                try {
                    return NormalizedNodeAggregator.aggregate(YangInstanceIdentifier.EMPTY, input, txContextFactory.getActorContext().getSchemaContext());
                } catch (DataValidationFailedException e) {
                    throw new IllegalArgumentException("Failed to aggregate", e);
                }
            }
        });

        return MappingCheckedFuture.create(aggregateFuture, ReadFailedException.MAPPER);
    }

    @Override
    public void delete(final YangInstanceIdentifier path) {
        checkModificationState();

        LOG.debug("Tx {} delete {}", getIdentifier(), path);

        limiter.throttleOperation();

        TransactionContextWrapper contextAdapter = getContextAdapter(path);
        contextAdapter.maybeExecuteTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.deleteData(path);
            }
        });
    }

    @Override
    public void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkModificationState();

        LOG.debug("Tx {} merge {}", getIdentifier(), path);

        limiter.throttleOperation();

        TransactionContextWrapper contextAdapter = getContextAdapter(path);
        contextAdapter.maybeExecuteTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.mergeData(path, data);
            }
        });
    }

    @Override
    public void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkModificationState();

        LOG.debug("Tx {} write {}", getIdentifier(), path);

        limiter.throttleOperation();

        TransactionContextWrapper contextAdapter = getContextAdapter(path);
        contextAdapter.maybeExecuteTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.writeData(path, data);
            }
        });
    }

    private void checkModificationState() {
        Preconditions.checkState(type != TransactionType.READ_ONLY,
                "Modification operation on read-only transaction is not allowed");
        Preconditions.checkState(state == TransactionState.OPEN,
                "Transaction is sealed - further modifications are not allowed");
    }

    private boolean seal(final TransactionState newState) {
        if (state == TransactionState.OPEN) {
            state = newState;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public final void close() {
        if (!seal(TransactionState.CLOSED)) {
            Preconditions.checkState(state == TransactionState.CLOSED, "Transaction %s is ready, it cannot be closed",
                getIdentifier());
            // Idempotent no-op as per AutoCloseable recommendation
            return;
        }

        for (TransactionContextWrapper contextAdapter : txContextAdapters.values()) {
            contextAdapter.maybeExecuteTransactionOperation(new TransactionOperation() {
                @Override
                public void invoke(TransactionContext transactionContext) {
                    transactionContext.closeTransaction();
                }
            });
        }


        txContextAdapters.clear();
    }

    @Override
    public final AbstractThreePhaseCommitCohort<?> ready() {
        Preconditions.checkState(type != TransactionType.READ_ONLY, "Read-only transactions cannot be readied");

        final boolean success = seal(TransactionState.READY);
        Preconditions.checkState(success, "Transaction %s is %s, it cannot be readied", getIdentifier(), state);

        LOG.debug("Tx {} Readying {} components for commit", getIdentifier(), txContextAdapters.size());

        final AbstractThreePhaseCommitCohort<?> ret;
        switch (txContextAdapters.size()) {
        case 0:
            ret = NoOpDOMStoreThreePhaseCommitCohort.INSTANCE;
            break;
        case 1:
            final Entry<String, TransactionContextWrapper> e = Iterables.getOnlyElement(txContextAdapters.entrySet());
            ret = createSingleCommitCohort(e.getKey(), e.getValue());
            break;
        default:
            ret = createMultiCommitCohort(txContextAdapters.entrySet());
        }

        txContextFactory.onTransactionReady(getIdentifier(), ret.getCohortFutures());
        return ret;
    }

    private AbstractThreePhaseCommitCohort<?> createSingleCommitCohort(final String shardName,
            final TransactionContextWrapper contextAdapter) {
        limiter.throttleOperation();

        LOG.debug("Tx {} Readying transaction for shard {}", getIdentifier(), shardName);

        final OperationCallback.Reference operationCallbackRef =
                new OperationCallback.Reference(OperationCallback.NO_OP_CALLBACK);

        final TransactionContext transactionContext = contextAdapter.getTransactionContext();
        final Future future;
        if (transactionContext == null) {
            final Promise promise = akka.dispatch.Futures.promise();
            contextAdapter.maybeExecuteTransactionOperation(new TransactionOperation() {
                @Override
                public void invoke(TransactionContext transactionContext) {
                    promise.completeWith(getReadyOrDirectCommitFuture(transactionContext, operationCallbackRef));
                }
            });
            future = promise.future();
        } else {
            // avoid the creation of a promise and a TransactionOperation
            future = getReadyOrDirectCommitFuture(transactionContext, operationCallbackRef);
        }

        return new SingleCommitCohortProxy(txContextFactory.getActorContext(), future, getIdentifier().toString(),
                operationCallbackRef);
    }

    private Future<?> getReadyOrDirectCommitFuture(TransactionContext transactionContext,
            OperationCallback.Reference operationCallbackRef) {
        if (transactionContext.supportsDirectCommit()) {
            TransactionRateLimitingCallback rateLimitingCallback = new TransactionRateLimitingCallback(
                    txContextFactory.getActorContext());
            operationCallbackRef.set(rateLimitingCallback);
            rateLimitingCallback.run();
            return transactionContext.directCommit();
        } else {
            return transactionContext.readyTransaction();
        }
    }

    private AbstractThreePhaseCommitCohort<ActorSelection> createMultiCommitCohort(
            final Set<Entry<String, TransactionContextWrapper>> txContextAdapterEntries) {

        limiter.throttleOperation();
        final List<Future<ActorSelection>> cohortFutures = new ArrayList<>(txContextAdapterEntries.size());
        for (Entry<String, TransactionContextWrapper> e : txContextAdapterEntries) {
            LOG.debug("Tx {} Readying transaction for shard {}", getIdentifier(), e.getKey());

            TransactionContextWrapper contextAdapter = e.getValue();
            final TransactionContext transactionContext = contextAdapter.getTransactionContext();
            Future<ActorSelection> future;
            if (transactionContext != null) {
                // avoid the creation of a promise and a TransactionOperation
                future = transactionContext.readyTransaction();
            } else {
                final Promise<ActorSelection> promise = akka.dispatch.Futures.promise();
                contextAdapter.maybeExecuteTransactionOperation(new TransactionOperation() {
                    @Override
                    public void invoke(TransactionContext transactionContext) {
                        promise.completeWith(transactionContext.readyTransaction());
                    }
                });

                future = promise.future();
            }

            cohortFutures.add(future);
        }

        return new ThreePhaseCommitCohortProxy(txContextFactory.getActorContext(), cohortFutures, getIdentifier().toString());
    }

    private static String shardNameFromIdentifier(final YangInstanceIdentifier path) {
        return ShardStrategyFactory.getStrategy(path).findShard(path);
    }

    private TransactionContextWrapper getContextAdapter(final YangInstanceIdentifier path) {
        return getContextAdapter(shardNameFromIdentifier(path));
    }

    private TransactionContextWrapper getContextAdapter(final String shardName) {
        final TransactionContextWrapper existing = txContextAdapters.get(shardName);
        if (existing != null) {
            return existing;
        }

        final TransactionContextWrapper fresh = txContextFactory.newTransactionAdapter(this, shardName);
        txContextAdapters.put(shardName, fresh);
        return fresh;
    }

    TransactionType getType() {
        return type;
    }

    boolean isReady() {
        return state != TransactionState.OPEN;
    }

    ActorContext getActorContext() {
        return txContextFactory.getActorContext();
    }

    OperationLimiter getLimiter() {
        return limiter;
    }
}
