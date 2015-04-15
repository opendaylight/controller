/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.cluster.datastore.compat.PreLithiumTransactionContextImpl;
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
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * TransactionProxy acts as a proxy for one or more transactions that were created on a remote shard
 * <p>
 * Creating a transaction on the consumer side will create one instance of a transaction proxy. If during
 * the transaction reads and writes are done on data that belongs to different shards then a separate transaction will
 * be created on each of those shards by the TransactionProxy
 *</p>
 * <p>
 * The TransactionProxy does not make any guarantees about atomicity or order in which the transactions on the various
 * shards will be executed.
 * </p>
 */
public class TransactionProxy extends AbstractDOMStoreTransaction<TransactionIdentifier> implements DOMStoreReadWriteTransaction {

    public static enum TransactionType {
        READ_ONLY,
        WRITE_ONLY,
        READ_WRITE;

        // Cache all values
        private static final TransactionType[] VALUES = values();

        public static TransactionType fromInt(final int type) {
            try {
                return VALUES[type];
            } catch (IndexOutOfBoundsException e) {
                throw new IllegalArgumentException("In TransactionType enum value " + type, e);
            }
        }
    }

    private static enum TransactionState {
        OPEN,
        READY,
        CLOSED,
    }

    static final Mapper<Throwable, Throwable> SAME_FAILURE_TRANSFORMER =
                                                              new Mapper<Throwable, Throwable>() {
        @Override
        public Throwable apply(Throwable failure) {
            return failure;
        }
    };

    private static final AtomicLong counter = new AtomicLong();

    private static final Logger LOG = LoggerFactory.getLogger(TransactionProxy.class);

    /**
     * Stores the remote Tx actors for each requested data store path to be used by the
     * PhantomReference to close the remote Tx's. This is only used for read-only Tx's. The
     * remoteTransactionActorsMB volatile serves as a memory barrier to publish updates to the
     * remoteTransactionActors list so they will be visible to the thread accessing the
     * PhantomReference.
     */
    List<ActorSelection> remoteTransactionActors;
    volatile AtomicBoolean remoteTransactionActorsMB;

    /**
     * Stores the create transaction results per shard.
     */
    private final Map<String, TransactionFutureCallback> txFutureCallbackMap = new HashMap<>();

    private final TransactionType transactionType;
    final ActorContext actorContext;
    private final String transactionChainId;
    private final SchemaContext schemaContext;
    private TransactionState state = TransactionState.OPEN;

    private volatile boolean initialized;
    private Semaphore operationLimiter;
    private OperationCompleter operationCompleter;

    public TransactionProxy(ActorContext actorContext, TransactionType transactionType) {
        this(actorContext, transactionType, "");
    }

    public TransactionProxy(ActorContext actorContext, TransactionType transactionType, String transactionChainId) {
        super(createIdentifier(actorContext));
        this.actorContext = Preconditions.checkNotNull(actorContext,
            "actorContext should not be null");
        this.transactionType = Preconditions.checkNotNull(transactionType,
            "transactionType should not be null");
        this.schemaContext = Preconditions.checkNotNull(actorContext.getSchemaContext(),
            "schemaContext should not be null");
        this.transactionChainId = transactionChainId;

        LOG.debug("Created txn {} of type {} on chain {}", getIdentifier(), transactionType, transactionChainId);
    }

    private static TransactionIdentifier createIdentifier(ActorContext actorContext) {
        String memberName = actorContext.getCurrentMemberName();
        if (memberName == null) {
            memberName = "UNKNOWN-MEMBER";
        }

        return new TransactionIdentifier(memberName, counter.getAndIncrement());
    }

    @VisibleForTesting
    boolean hasTransactionContext() {
        for(TransactionFutureCallback txFutureCallback : txFutureCallbackMap.values()) {
            TransactionContext transactionContext = txFutureCallback.getTransactionContext();
            if(transactionContext != null) {
                return true;
            }
        }

        return false;
    }

    private boolean isRootPath(YangInstanceIdentifier path){
        return !path.getPathArguments().iterator().hasNext();
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final YangInstanceIdentifier path) {

        Preconditions.checkState(transactionType != TransactionType.WRITE_ONLY,
                "Read operation on write-only transaction is not allowed");

        LOG.debug("Tx {} read {}", getIdentifier(), path);

        final SettableFuture<Optional<NormalizedNode<?, ?>>> proxyFuture = SettableFuture.create();

        if(isRootPath(path)){
            readAllData(path, proxyFuture);
        } else {
            throttleOperation();

            TransactionFutureCallback txFutureCallback = getOrCreateTxFutureCallback(path);
            txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
                @Override
                public void invoke(TransactionContext transactionContext) {
                    transactionContext.readData(path, proxyFuture);
                }
            });

        }

        return MappingCheckedFuture.create(proxyFuture, ReadFailedException.MAPPER);
    }

    private void readAllData(final YangInstanceIdentifier path,
                             final SettableFuture<Optional<NormalizedNode<?, ?>>> proxyFuture) {
        Set<String> allShardNames = actorContext.getConfiguration().getAllShardNames();
        List<SettableFuture<Optional<NormalizedNode<?, ?>>>> futures = new ArrayList<>(allShardNames.size());

        for(String shardName : allShardNames){
            final SettableFuture<Optional<NormalizedNode<?, ?>>> subProxyFuture = SettableFuture.create();

            throttleOperation();

            TransactionFutureCallback txFutureCallback = getOrCreateTxFutureCallback(shardName);
            txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
                @Override
                public void invoke(TransactionContext transactionContext) {
                    transactionContext.readData(path, subProxyFuture);
                }
            });

            futures.add(subProxyFuture);
        }

        final ListenableFuture<List<Optional<NormalizedNode<?, ?>>>> future = Futures.allAsList(futures);

        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    proxyFuture.set(NormalizedNodeAggregator.aggregate(YangInstanceIdentifier.builder().build(),
                            future.get(), actorContext.getSchemaContext()));
                } catch (InterruptedException | ExecutionException e) {
                    proxyFuture.setException(e);
                }
            }
        }, actorContext.getActorSystem().dispatcher());
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {

        Preconditions.checkState(transactionType != TransactionType.WRITE_ONLY,
                "Exists operation on write-only transaction is not allowed");

        LOG.debug("Tx {} exists {}", getIdentifier(), path);

        throttleOperation();

        final SettableFuture<Boolean> proxyFuture = SettableFuture.create();

        TransactionFutureCallback txFutureCallback = getOrCreateTxFutureCallback(path);
        txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.dataExists(path, proxyFuture);
            }
        });

        return MappingCheckedFuture.create(proxyFuture, ReadFailedException.MAPPER);
    }

    private void checkModificationState() {
        Preconditions.checkState(transactionType != TransactionType.READ_ONLY,
                "Modification operation on read-only transaction is not allowed");
        Preconditions.checkState(state == TransactionState.OPEN,
                "Transaction is sealed - further modifications are not allowed");
    }

    private void throttleOperation() {
        throttleOperation(1);
    }

    private void throttleOperation(int acquirePermits) {
        if(!initialized) {
            // Note : Currently mailbox-capacity comes from akka.conf and not from the config-subsystem
            operationLimiter = new Semaphore(actorContext.getTransactionOutstandingOperationLimit());
            operationCompleter = new OperationCompleter(operationLimiter);

            // Make sure we write this last because it's volatile and will also publish the non-volatile writes
            // above as well so they'll be visible to other threads.
            initialized = true;
        }

        try {
            if(!operationLimiter.tryAcquire(acquirePermits,
                    actorContext.getDatastoreContext().getOperationTimeoutInSeconds(), TimeUnit.SECONDS)){
                LOG.warn("Failed to acquire operation permit for transaction {}", getIdentifier());
            }
        } catch (InterruptedException e) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Interrupted when trying to acquire operation permit for transaction " + getIdentifier().toString(), e);
            } else {
                LOG.warn("Interrupted when trying to acquire operation permit for transaction {}", getIdentifier());
            }
        }
    }

    final void ensureInitializied() {
        Preconditions.checkState(initialized, "Transaction %s was not propertly initialized.", getIdentifier());
    }

    @Override
    public void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {

        checkModificationState();

        LOG.debug("Tx {} write {}", getIdentifier(), path);

        throttleOperation();

        TransactionFutureCallback txFutureCallback = getOrCreateTxFutureCallback(path);
        txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.writeData(path, data);
            }
        });
    }

    @Override
    public void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {

        checkModificationState();

        LOG.debug("Tx {} merge {}", getIdentifier(), path);

        throttleOperation();

        TransactionFutureCallback txFutureCallback = getOrCreateTxFutureCallback(path);
        txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.mergeData(path, data);
            }
        });
    }

    @Override
    public void delete(final YangInstanceIdentifier path) {

        checkModificationState();

        LOG.debug("Tx {} delete {}", getIdentifier(), path);

        throttleOperation();

        TransactionFutureCallback txFutureCallback = getOrCreateTxFutureCallback(path);
        txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.deleteData(path);
            }
        });
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
    public AbstractThreePhaseCommitCohort<?> ready() {
        Preconditions.checkState(transactionType != TransactionType.READ_ONLY,
                "Read-only transactions cannot be readied");

        final boolean success = seal(TransactionState.READY);
        Preconditions.checkState(success, "Transaction %s is %s, it cannot be readied", getIdentifier(), state);

        LOG.debug("Tx {} Readying {} transactions for commit", getIdentifier(),
                    txFutureCallbackMap.size());

        if (txFutureCallbackMap.isEmpty()) {
            TransactionRateLimitingCallback.adjustRateLimitForUnusedTransaction(actorContext);
            return NoOpDOMStoreThreePhaseCommitCohort.INSTANCE;
        }

        throttleOperation(txFutureCallbackMap.size());

        final boolean isSingleShard = txFutureCallbackMap.size() == 1;
        return isSingleShard ? createSingleCommitCohort() : createMultiCommitCohort();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private AbstractThreePhaseCommitCohort<Object> createSingleCommitCohort() {
        TransactionFutureCallback txFutureCallback = txFutureCallbackMap.values().iterator().next();

        LOG.debug("Tx {} Readying transaction for shard {} on chain {}", getIdentifier(),
                txFutureCallback.getShardName(), transactionChainId);

        final TransactionContext transactionContext = txFutureCallback.getTransactionContext();
        final Future future;
        if (transactionContext != null) {
            // avoid the creation of a promise and a TransactionOperation
            future = getReadyOrDirectCommitFuture(transactionContext);
        } else {
            final Promise promise = akka.dispatch.Futures.promise();
            txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
                @Override
                public void invoke(TransactionContext transactionContext) {
                    promise.completeWith(getReadyOrDirectCommitFuture(transactionContext));
                }
            });
            future = promise.future();
        }

        return new SingleCommitCohortProxy(actorContext, future, getIdentifier().toString());
    }

    private Future<?> getReadyOrDirectCommitFuture(TransactionContext transactionContext) {
        return transactionContext.supportsDirectCommit() ? transactionContext.directCommit() :
            transactionContext.readyTransaction();
    }

    private AbstractThreePhaseCommitCohort<ActorSelection> createMultiCommitCohort() {
        List<Future<ActorSelection>> cohortFutures = new ArrayList<>(txFutureCallbackMap.size());
        for(TransactionFutureCallback txFutureCallback : txFutureCallbackMap.values()) {

            LOG.debug("Tx {} Readying transaction for shard {} on chain {}", getIdentifier(),
                        txFutureCallback.getShardName(), transactionChainId);

            final TransactionContext transactionContext = txFutureCallback.getTransactionContext();
            final Future<ActorSelection> future;
            if (transactionContext != null) {
                // avoid the creation of a promise and a TransactionOperation
                future = transactionContext.readyTransaction();
            } else {
                final Promise<ActorSelection> promise = akka.dispatch.Futures.promise();
                txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
                    @Override
                    public void invoke(TransactionContext transactionContext) {
                        promise.completeWith(transactionContext.readyTransaction());
                    }
                });
                future = promise.future();
            }

            cohortFutures.add(future);
        }

        return new ThreePhaseCommitCohortProxy(actorContext, cohortFutures, getIdentifier().toString());
    }

    @Override
    public void close() {
        if (!seal(TransactionState.CLOSED)) {
            if (state == TransactionState.CLOSED) {
                // Idempotent no-op as per AutoCloseable recommendation
                return;
            }

            throw new IllegalStateException(String.format("Transaction %s is ready, it cannot be closed",
                getIdentifier()));
        }

        for (TransactionFutureCallback txFutureCallback : txFutureCallbackMap.values()) {
            txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
                @Override
                public void invoke(TransactionContext transactionContext) {
                    transactionContext.closeTransaction();
                }
            });
        }

        txFutureCallbackMap.clear();

        if(remoteTransactionActorsMB != null) {
            remoteTransactionActors.clear();
            remoteTransactionActorsMB.set(true);
        }
    }

    private String shardNameFromIdentifier(YangInstanceIdentifier path){
        return ShardStrategyFactory.getStrategy(path).findShard(path);
    }

    protected Future<ActorSelection> sendFindPrimaryShardAsync(String shardName) {
        return actorContext.findPrimaryShardAsync(shardName);
    }

    final TransactionType getTransactionType() {
        return transactionType;
    }

    final Semaphore getOperationLimiter() {
        return operationLimiter;
    }

    private TransactionFutureCallback getOrCreateTxFutureCallback(YangInstanceIdentifier path) {
        String shardName = shardNameFromIdentifier(path);
        return getOrCreateTxFutureCallback(shardName);
    }

    private TransactionFutureCallback getOrCreateTxFutureCallback(String shardName) {
        TransactionFutureCallback txFutureCallback = txFutureCallbackMap.get(shardName);
        if(txFutureCallback == null) {
            Future<ActorSelection> findPrimaryFuture = sendFindPrimaryShardAsync(shardName);

            final TransactionFutureCallback newTxFutureCallback = new TransactionFutureCallback(this, shardName);

            txFutureCallback = newTxFutureCallback;
            txFutureCallbackMap.put(shardName, txFutureCallback);

            findPrimaryFuture.onComplete(new OnComplete<ActorSelection>() {
                @Override
                public void onComplete(Throwable failure, ActorSelection primaryShard) {
                    if(failure != null) {
                        newTxFutureCallback.createTransactionContext(failure, null);
                    } else {
                        newTxFutureCallback.setPrimaryShard(primaryShard);
                    }
                }
            }, actorContext.getClientDispatcher());
        }

        return txFutureCallback;
    }

    String getTransactionChainId() {
        return transactionChainId;
    }

    protected ActorContext getActorContext() {
        return actorContext;
    }

    TransactionContext createValidTransactionContext(ActorSelection transactionActor,
            String transactionPath, short remoteTransactionVersion) {

        if (transactionType == TransactionType.READ_ONLY) {
            // Read-only Tx's aren't explicitly closed by the client so we create a PhantomReference
            // to close the remote Tx's when this instance is no longer in use and is garbage
            // collected.

            if(remoteTransactionActorsMB == null) {
                remoteTransactionActors = Lists.newArrayList();
                remoteTransactionActorsMB = new AtomicBoolean();

                TransactionProxyCleanupPhantomReference.track(TransactionProxy.this);
            }

            // Add the actor to the remoteTransactionActors list for access by the
            // cleanup PhantonReference.
            remoteTransactionActors.add(transactionActor);

            // Write to the memory barrier volatile to publish the above update to the
            // remoteTransactionActors list for thread visibility.
            remoteTransactionActorsMB.set(true);
        }

        // TxActor is always created where the leader of the shard is.
        // Check if TxActor is created in the same node
        boolean isTxActorLocal = actorContext.isPathLocal(transactionPath);

        if(remoteTransactionVersion < DataStoreVersions.LITHIUM_VERSION) {
            return new PreLithiumTransactionContextImpl(transactionPath, transactionActor, getIdentifier(),
                    transactionChainId, actorContext, schemaContext, isTxActorLocal, remoteTransactionVersion,
                    operationCompleter);
        } else {
            return new TransactionContextImpl(transactionActor, getIdentifier(), transactionChainId,
                    actorContext, schemaContext, isTxActorLocal, remoteTransactionVersion, operationCompleter);
        }
    }
}
