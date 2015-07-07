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
import akka.pattern.AskTimeoutException;
import akka.util.Timeout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.FinalizablePhantomReference;
import com.google.common.base.FinalizableReferenceQueue;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.util.concurrent.MappingCheckedFuture;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.concurrent.duration.FiniteDuration;

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
public class TransactionProxy implements DOMStoreReadWriteTransaction {

    public static enum TransactionType {
        READ_ONLY,
        WRITE_ONLY,
        READ_WRITE
    }

    private static final long MAX_CREATE_TX_MSG_TIMEOUT_IN_MS = 5000;

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
     * Used to enqueue the PhantomReferences for read-only TransactionProxy instances. The
     * FinalizableReferenceQueue is safe to use statically in an OSGi environment as it uses some
     * trickery to clean up its internal thread when the bundle is unloaded.
     */
    private static final FinalizableReferenceQueue phantomReferenceQueue =
                                                                  new FinalizableReferenceQueue();

    /**
     * This stores the TransactionProxyCleanupPhantomReference instances statically, This is
     * necessary because PhantomReferences need a hard reference so they're not garbage collected.
     * Once finalized, the TransactionProxyCleanupPhantomReference removes itself from this map
     * and thus becomes eligible for garbage collection.
     */
    private static final Map<TransactionProxyCleanupPhantomReference,
                             TransactionProxyCleanupPhantomReference> phantomReferenceCache =
                                                                        new ConcurrentHashMap<>();

    /**
     * A PhantomReference that closes remote transactions for a TransactionProxy when it's
     * garbage collected. This is used for read-only transactions as they're not explicitly closed
     * by clients. So the only way to detect that a transaction is no longer in use and it's safe
     * to clean up is when it's garbage collected. It's inexact as to when an instance will be GC'ed
     * but TransactionProxy instances should generally be short-lived enough to avoid being moved
     * to the old generation space and thus should be cleaned up in a timely manner as the GC
     * runs on the young generation (eden, swap1...) space much more frequently.
     */
    private static class TransactionProxyCleanupPhantomReference
                                           extends FinalizablePhantomReference<TransactionProxy> {

        private final List<ActorSelection> remoteTransactionActors;
        private final AtomicBoolean remoteTransactionActorsMB;
        private final ActorContext actorContext;
        private final TransactionIdentifier identifier;

        protected TransactionProxyCleanupPhantomReference(TransactionProxy referent) {
            super(referent, phantomReferenceQueue);

            // Note we need to cache the relevant fields from the TransactionProxy as we can't
            // have a hard reference to the TransactionProxy instance itself.

            remoteTransactionActors = referent.remoteTransactionActors;
            remoteTransactionActorsMB = referent.remoteTransactionActorsMB;
            actorContext = referent.actorContext;
            identifier = referent.identifier;
        }

        @Override
        public void finalizeReferent() {
            LOG.trace("Cleaning up {} Tx actors for TransactionProxy {}",
                    remoteTransactionActors.size(), identifier);

            phantomReferenceCache.remove(this);

            // Access the memory barrier volatile to ensure all previous updates to the
            // remoteTransactionActors list are visible to this thread.

            if(remoteTransactionActorsMB.get()) {
                for(ActorSelection actor : remoteTransactionActors) {
                    LOG.trace("Sending CloseTransaction to {}", actor);
                    actorContext.sendOperationAsync(actor,
                            new CloseTransaction().toSerializable());
                }
            }
        }
    }

    /**
     * Stores the remote Tx actors for each requested data store path to be used by the
     * PhantomReference to close the remote Tx's. This is only used for read-only Tx's. The
     * remoteTransactionActorsMB volatile serves as a memory barrier to publish updates to the
     * remoteTransactionActors list so they will be visible to the thread accessing the
     * PhantomReference.
     */
    private List<ActorSelection> remoteTransactionActors;
    private AtomicBoolean remoteTransactionActorsMB;

    /**
     * Stores the create transaction results per shard.
     */
    private final Map<String, TransactionFutureCallback> txFutureCallbackMap = new HashMap<>();

    private final TransactionType transactionType;
    private final ActorContext actorContext;
    private final TransactionIdentifier identifier;
    private final String transactionChainId;
    private final SchemaContext schemaContext;
    private boolean inReadyState;

    public TransactionProxy(ActorContext actorContext, TransactionType transactionType) {
        this(actorContext, transactionType, "");
    }

    public TransactionProxy(ActorContext actorContext, TransactionType transactionType,
            String transactionChainId) {
        this.actorContext = Preconditions.checkNotNull(actorContext,
            "actorContext should not be null");
        this.transactionType = Preconditions.checkNotNull(transactionType,
            "transactionType should not be null");
        this.schemaContext = Preconditions.checkNotNull(actorContext.getSchemaContext(),
            "schemaContext should not be null");
        this.transactionChainId = transactionChainId;

        String memberName = actorContext.getCurrentMemberName();
        if(memberName == null){
            memberName = "UNKNOWN-MEMBER";
        }

        this.identifier = TransactionIdentifier.builder().memberName(memberName).counter(
            counter.getAndIncrement()).build();

        if(transactionType == TransactionType.READ_ONLY) {
            // Read-only Tx's aren't explicitly closed by the client so we create a PhantomReference
            // to close the remote Tx's when this instance is no longer in use and is garbage
            // collected.

            remoteTransactionActors = Lists.newArrayList();
            remoteTransactionActorsMB = new AtomicBoolean();

            TransactionProxyCleanupPhantomReference cleanup =
                new TransactionProxyCleanupPhantomReference(this);
            phantomReferenceCache.put(cleanup, cleanup);
        }

        LOG.debug("Created txn {} of type {} on chain {}", identifier, transactionType, transactionChainId);
    }

    @VisibleForTesting
    List<Future<Object>> getRecordedOperationFutures() {
        List<Future<Object>> recordedOperationFutures = Lists.newArrayList();
        for(TransactionFutureCallback txFutureCallback : txFutureCallbackMap.values()) {
            TransactionContext transactionContext = txFutureCallback.getTransactionContext();
            if(transactionContext != null) {
                recordedOperationFutures.addAll(transactionContext.getRecordedOperationFutures());
            }
        }

        return recordedOperationFutures;
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

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
            final YangInstanceIdentifier path) {

        Preconditions.checkState(transactionType != TransactionType.WRITE_ONLY,
                "Read operation on write-only transaction is not allowed");

        LOG.debug("Tx {} read {}", identifier, path);

        TransactionFutureCallback txFutureCallback = getOrCreateTxFutureCallback(path);
        TransactionContext transactionContext = txFutureCallback.getTransactionContext();

        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> future;
        if(transactionContext != null) {
            future = transactionContext.readData(path);
        } else {
            // The shard Tx hasn't been created yet so add the Tx operation to the Tx Future
            // callback to be executed after the Tx is created.
            final SettableFuture<Optional<NormalizedNode<?, ?>>> proxyFuture = SettableFuture.create();
            txFutureCallback.addTxOperationOnComplete(new TransactionOperation() {
                @Override
                public void invoke(TransactionContext transactionContext) {
                    Futures.addCallback(transactionContext.readData(path),
                        new FutureCallback<Optional<NormalizedNode<?, ?>>>() {
                            @Override
                            public void onSuccess(Optional<NormalizedNode<?, ?>> data) {
                                proxyFuture.set(data);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                proxyFuture.setException(t);
                            }
                        });
                }
            });

            future = MappingCheckedFuture.create(proxyFuture, ReadFailedException.MAPPER);
        }

        return future;
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {

        Preconditions.checkState(transactionType != TransactionType.WRITE_ONLY,
                "Exists operation on write-only transaction is not allowed");

        LOG.debug("Tx {} exists {}", identifier, path);

        TransactionFutureCallback txFutureCallback = getOrCreateTxFutureCallback(path);
        TransactionContext transactionContext = txFutureCallback.getTransactionContext();

        CheckedFuture<Boolean, ReadFailedException> future;
        if(transactionContext != null) {
            future = transactionContext.dataExists(path);
        } else {
            // The shard Tx hasn't been created yet so add the Tx operation to the Tx Future
            // callback to be executed after the Tx is created.
            final SettableFuture<Boolean> proxyFuture = SettableFuture.create();
            txFutureCallback.addTxOperationOnComplete(new TransactionOperation() {
                @Override
                public void invoke(TransactionContext transactionContext) {
                    Futures.addCallback(transactionContext.dataExists(path),
                        new FutureCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean exists) {
                                proxyFuture.set(exists);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                proxyFuture.setException(t);
                            }
                        });
                }
            });

            future = MappingCheckedFuture.create(proxyFuture, ReadFailedException.MAPPER);
        }

        return future;
    }

    private void checkModificationState() {
        Preconditions.checkState(transactionType != TransactionType.READ_ONLY,
                "Modification operation on read-only transaction is not allowed");
        Preconditions.checkState(!inReadyState,
                "Transaction is sealed - further modifications are not allowed");
    }

    @Override
    public void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {

        checkModificationState();

        LOG.debug("Tx {} write {}", identifier, path);

        TransactionFutureCallback txFutureCallback = getOrCreateTxFutureCallback(path);
        TransactionContext transactionContext = txFutureCallback.getTransactionContext();
        if(transactionContext != null) {
            transactionContext.writeData(path, data);
        } else {
            // The shard Tx hasn't been created yet so add the Tx operation to the Tx Future
            // callback to be executed after the Tx is created.
            txFutureCallback.addTxOperationOnComplete(new TransactionOperation() {
                @Override
                public void invoke(TransactionContext transactionContext) {
                    transactionContext.writeData(path, data);
                }
            });
        }
    }

    @Override
    public void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {

        checkModificationState();

        LOG.debug("Tx {} merge {}", identifier, path);

        TransactionFutureCallback txFutureCallback = getOrCreateTxFutureCallback(path);
        TransactionContext transactionContext = txFutureCallback.getTransactionContext();
        if(transactionContext != null) {
            transactionContext.mergeData(path, data);
        } else {
            // The shard Tx hasn't been created yet so add the Tx operation to the Tx Future
            // callback to be executed after the Tx is created.
            txFutureCallback.addTxOperationOnComplete(new TransactionOperation() {
                @Override
                public void invoke(TransactionContext transactionContext) {
                    transactionContext.mergeData(path, data);
                }
            });
        }
    }

    @Override
    public void delete(final YangInstanceIdentifier path) {

        checkModificationState();

        LOG.debug("Tx {} delete {}", identifier, path);

        TransactionFutureCallback txFutureCallback = getOrCreateTxFutureCallback(path);
        TransactionContext transactionContext = txFutureCallback.getTransactionContext();
        if(transactionContext != null) {
            transactionContext.deleteData(path);
        } else {
            // The shard Tx hasn't been created yet so add the Tx operation to the Tx Future
            // callback to be executed after the Tx is created.
            txFutureCallback.addTxOperationOnComplete(new TransactionOperation() {
                @Override
                public void invoke(TransactionContext transactionContext) {
                    transactionContext.deleteData(path);
                }
            });
        }
    }

    @Override
    public DOMStoreThreePhaseCommitCohort ready() {

        checkModificationState();

        inReadyState = true;

        LOG.debug("Tx {} Readying {} transactions for commit", identifier,
                    txFutureCallbackMap.size());

        List<Future<ActorSelection>> cohortFutures = Lists.newArrayList();

        for(TransactionFutureCallback txFutureCallback : txFutureCallbackMap.values()) {

            LOG.debug("Tx {} Readying transaction for shard {} chain {}", identifier,
                        txFutureCallback.getShardName(), transactionChainId);

            TransactionContext transactionContext = txFutureCallback.getTransactionContext();
            if(transactionContext != null) {
                cohortFutures.add(transactionContext.readyTransaction());
            } else {
                // The shard Tx hasn't been created yet so create a promise to ready the Tx later
                // after it's created.
                final Promise<ActorSelection> cohortPromise = akka.dispatch.Futures.promise();
                txFutureCallback.addTxOperationOnComplete(new TransactionOperation() {
                    @Override
                    public void invoke(TransactionContext transactionContext) {
                        cohortPromise.completeWith(transactionContext.readyTransaction());
                    }
                });

                cohortFutures.add(cohortPromise.future());
            }
        }

        onTransactionReady(cohortFutures);

        return new ThreePhaseCommitCohortProxy(actorContext, cohortFutures,
                identifier.toString());
    }

    /**
     * Method for derived classes to be notified when the transaction has been readied.
     *
     * @param cohortFutures the cohort Futures for each shard transaction.
     */
    protected void onTransactionReady(List<Future<ActorSelection>> cohortFutures) {
    }

    /**
     * Method called to send a CreateTransaction message to a shard.
     *
     * @param shard the shard actor to send to
     * @param serializedCreateMessage the serialized message to send
     * @return the response Future
     */
    protected Future<Object> sendCreateTransaction(ActorSelection shard,
            Object serializedCreateMessage) {
        return actorContext.executeOperationAsync(shard, serializedCreateMessage,
                new Timeout(getCreateTxMessageTimeoutInMillis(), TimeUnit.MILLISECONDS));
    }

    private long getCreateTxMessageTimeoutInMillis() {
        long operationTimeout = actorContext.getOperationTimeout().duration().toMillis();
        return operationTimeout > MAX_CREATE_TX_MSG_TIMEOUT_IN_MS ?
                MAX_CREATE_TX_MSG_TIMEOUT_IN_MS : operationTimeout;
    }

    @Override
    public Object getIdentifier() {
        return this.identifier;
    }

    @Override
    public void close() {
        for(TransactionFutureCallback txFutureCallback : txFutureCallbackMap.values()) {
            TransactionContext transactionContext = txFutureCallback.getTransactionContext();
            if(transactionContext != null) {
                transactionContext.closeTransaction();
            } else {
                txFutureCallback.addTxOperationOnComplete(new TransactionOperation() {
                    @Override
                    public void invoke(TransactionContext transactionContext) {
                        transactionContext.closeTransaction();
                    }
                });
            }
        }

        txFutureCallbackMap.clear();

        if(transactionType == TransactionType.READ_ONLY) {
            remoteTransactionActors.clear();
            remoteTransactionActorsMB.set(true);
        }
    }

    private String shardNameFromIdentifier(YangInstanceIdentifier path){
        return ShardStrategyFactory.getStrategy(path).findShard(path);
    }

    private TransactionFutureCallback getOrCreateTxFutureCallback(YangInstanceIdentifier path) {
        String shardName = shardNameFromIdentifier(path);
        TransactionFutureCallback txFutureCallback = txFutureCallbackMap.get(shardName);
        if(txFutureCallback == null) {
            Future<ActorSelection> findPrimaryFuture = actorContext.findPrimaryShardAsync(shardName);

            final TransactionFutureCallback newTxFutureCallback =
                    new TransactionFutureCallback(shardName);

            txFutureCallback = newTxFutureCallback;
            txFutureCallbackMap.put(shardName, txFutureCallback);

            findPrimaryFuture.onComplete(new OnComplete<ActorSelection>() {
                @Override
                public void onComplete(Throwable failure, ActorSelection primaryShard) {
                    if(failure != null) {
                        newTxFutureCallback.onComplete(failure, null);
                    } else {
                        newTxFutureCallback.setPrimaryShard(primaryShard);
                    }
                }
            }, actorContext.getActorSystem().dispatcher());
        }

        return txFutureCallback;
    }

    public String getTransactionChainId() {
        return transactionChainId;
    }

    protected ActorContext getActorContext() {
        return actorContext;
    }

    /**
     * Interface for a transaction operation to be invoked later.
     */
    private static interface TransactionOperation {
        void invoke(TransactionContext transactionContext);
    }

    /**
     * Implements a Future OnComplete callback for a CreateTransaction message. This class handles
     * retries, up to a limit, if the shard doesn't have a leader yet. This is done by scheduling a
     * retry task after a short delay.
     * <p>
     * The end result from a completed CreateTransaction message is a TransactionContext that is
     * used to perform transaction operations. Transaction operations that occur before the
     * CreateTransaction completes are cache and executed once the CreateTransaction completes,
     * successfully or not.
     */
    private class TransactionFutureCallback extends OnComplete<Object> {

        private static final int MAX_CREATE_TX_TRIES = 2;
        private static final long CREATE_TX_TRY_INTERVAL_IN_MS = 1000;

        /**
         * The list of transaction operations to execute once the CreateTransaction completes.
         */
        @GuardedBy("txOperationsOnComplete")
        private final List<TransactionOperation> txOperationsOnComplete = Lists.newArrayList();

        /**
         * The TransactionContext resulting from the CreateTransaction reply.
         */
        private volatile TransactionContext transactionContext;

        /**
         * The target primary shard.
         */
        private volatile ActorSelection primaryShard;

        /**
         * Create tx timeout is 3 times the election timeout.
         */
        private volatile long totalCreateTxTimeout = actorContext.getDatastoreContext().getShardRaftConfig()
                .getElectionTimeOutInterval().toMillis() * MAX_CREATE_TX_TRIES;

        private final String shardName;

        TransactionFutureCallback(String shardName) {
            this.shardName = shardName;
        }

        String getShardName() {
            return shardName;
        }

        TransactionContext getTransactionContext() {
            return transactionContext;
        }


        /**
         * Sets the target primary shard and initiates a CreateTransaction try.
         */
        void setPrimaryShard(ActorSelection primaryShard) {
            LOG.debug("Tx {} Primary shard found - trying create transaction", identifier);

            this.primaryShard = primaryShard;
            tryCreateTransaction();
        }

        /**
         * Adds a TransactionOperation to be executed after the CreateTransaction completes.
         */
        void addTxOperationOnComplete(TransactionOperation operation) {
            synchronized(txOperationsOnComplete) {
                if(transactionContext == null) {
                    LOG.debug("Tx {} Adding operation on complete {}", identifier);

                    txOperationsOnComplete.add(operation);
                } else {
                    operation.invoke(transactionContext);
                }
            }
        }

        /**
         * Performs a CreateTransaction try async.
         */
        private void tryCreateTransaction() {
            Future<Object> createTxFuture = sendCreateTransaction(primaryShard,
                    new CreateTransaction(identifier.toString(),
                            TransactionProxy.this.transactionType.ordinal(),
                            getTransactionChainId()).toSerializable());

            createTxFuture.onComplete(this, actorContext.getActorSystem().dispatcher());
        }

        @Override
        public void onComplete(Throwable failure, Object response) {
            // An AskTimeoutException will occur if the local shard forwards to a remote leader and
            // the remote leader isn't available.
            boolean retryCreateTransaction = primaryShard != null &&
                    (failure instanceof NoShardLeaderException || failure instanceof AskTimeoutException);
            if(retryCreateTransaction) {
                // Schedule a retry unless we're out of retries. Note: totalCreateTxTimeout is volatile as it may
                // be written by different threads however not concurrently, therefore decrementing it
                // non-atomically here is ok.
                if(totalCreateTxTimeout > 0) {
                    long scheduleInterval = CREATE_TX_TRY_INTERVAL_IN_MS;
                    if(failure instanceof AskTimeoutException) {
                        // Since we use the operationTimeout for the CreateTransaction request and it timed out,
                        // subtract it from the total timeout. Also since the operationTimeout period has already
                        // elapsed, we can immediately schedule the retry (10 ms is virtually immediate).
                        totalCreateTxTimeout -= getCreateTxMessageTimeoutInMillis();
                        scheduleInterval = 10;
                    }

                    totalCreateTxTimeout -= scheduleInterval;

                    LOG.debug("Tx {}: create tx on shard {} failed with exception {} - scheduling retry in {} ms",
                            identifier, shardName, failure.getClass().getName(), scheduleInterval);

                    actorContext.getActorSystem().scheduler().scheduleOnce(FiniteDuration.create(
                            scheduleInterval, TimeUnit.MILLISECONDS),
                            new Runnable() {
                                @Override
                                public void run() {
                                    tryCreateTransaction();
                                }
                            }, actorContext.getActorSystem().dispatcher());
                    return;
                }
            }

            // Create the TransactionContext from the response or failure and execute delayed
            // TransactionOperations. This entire section is done atomically (ie synchronized) with
            // respect to #addTxOperationOnComplete to handle timing issues and ensure no
            // TransactionOperation is missed and that they are processed in the order they occurred.
            synchronized(txOperationsOnComplete) {
                // Store the new TransactionContext locally until we've completed invoking the
                // TransactionOperations. This avoids thread timing issues which could cause
                // out-of-order TransactionOperations. Eg, on a modification operation, if the
                // TransactionContext is non-null, then we directly call the TransactionContext.
                // However, at the same time, the code may be executing the cached
                // TransactionOperations. So to avoid thus timing, we don't publish the
                // TransactionContext until after we've executed all cached TransactionOperations.
                TransactionContext localTransactionContext;
                if(failure != null) {
                    LOG.debug("Tx {} Creating NoOpTransaction because of error: {}", identifier,
                            failure.getMessage());

                    Throwable resultingEx = failure;
                    if(!(failure instanceof NoShardLeaderException)) {
                        resultingEx = new Exception(String.format(
                            "Error creating %s transaction on shard %s", transactionType, shardName), failure);
                    }
                    localTransactionContext = new NoOpTransactionContext(resultingEx, identifier);
                } else if (response.getClass().equals(CreateTransactionReply.SERIALIZABLE_CLASS)) {
                    localTransactionContext = createValidTransactionContext(
                            CreateTransactionReply.fromSerializable(response));
                } else {
                    IllegalArgumentException exception = new IllegalArgumentException(String.format(
                        "Invalid reply type %s for CreateTransaction", response.getClass()));

                    localTransactionContext = new NoOpTransactionContext(exception, identifier);
                }

                for(TransactionOperation oper: txOperationsOnComplete) {
                    oper.invoke(localTransactionContext);
                }

                txOperationsOnComplete.clear();

                // We're done invoking the TransactionOperations so we can now publish the
                // TransactionContext.
                transactionContext = localTransactionContext;
            }
        }

        private TransactionContext createValidTransactionContext(CreateTransactionReply reply) {
            String transactionPath = reply.getTransactionPath();

            LOG.debug("Tx {} Received transaction actor path {}", identifier, transactionPath);

            ActorSelection transactionActor = actorContext.actorSelection(transactionPath);

            if (transactionType == TransactionType.READ_ONLY) {
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

            return new TransactionContextImpl(transactionPath, transactionActor, identifier,
                actorContext, schemaContext, isTxActorLocal, reply.getVersion());
        }
    }

    private interface TransactionContext {
        void closeTransaction();

        Future<ActorSelection> readyTransaction();

        void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data);

        void deleteData(YangInstanceIdentifier path);

        void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data);

        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readData(
                final YangInstanceIdentifier path);

        CheckedFuture<Boolean, ReadFailedException> dataExists(YangInstanceIdentifier path);

        List<Future<Object>> getRecordedOperationFutures();
    }

    private static abstract class AbstractTransactionContext implements TransactionContext {

        protected final TransactionIdentifier identifier;
        protected final List<Future<Object>> recordedOperationFutures = Lists.newArrayList();

        AbstractTransactionContext(TransactionIdentifier identifier) {
            this.identifier = identifier;
        }

        @Override
        public List<Future<Object>> getRecordedOperationFutures() {
            return recordedOperationFutures;
        }
    }

    private static class TransactionContextImpl extends AbstractTransactionContext {
        private final Logger LOG = LoggerFactory.getLogger(TransactionContextImpl.class);

        private final ActorContext actorContext;
        private final SchemaContext schemaContext;
        private final String transactionPath;
        private final ActorSelection actor;
        private final boolean isTxActorLocal;
        private final int remoteTransactionVersion;

        private TransactionContextImpl(String transactionPath, ActorSelection actor, TransactionIdentifier identifier,
                ActorContext actorContext, SchemaContext schemaContext,
                boolean isTxActorLocal, int remoteTransactionVersion) {
            super(identifier);
            this.transactionPath = transactionPath;
            this.actor = actor;
            this.actorContext = actorContext;
            this.schemaContext = schemaContext;
            this.isTxActorLocal = isTxActorLocal;
            this.remoteTransactionVersion = remoteTransactionVersion;
        }

        private ActorSelection getActor() {
            return actor;
        }

        @Override
        public void closeTransaction() {
            LOG.debug("Tx {} closeTransaction called", identifier);

            actorContext.sendOperationAsync(getActor(), new CloseTransaction().toSerializable());
        }

        @Override
        public Future<ActorSelection> readyTransaction() {
            LOG.debug("Tx {} readyTransaction called with {} previous recorded operations pending",
                    identifier, recordedOperationFutures.size());

            // Send the ReadyTransaction message to the Tx actor.

            ReadyTransaction readyTransaction = new ReadyTransaction();
            final Future<Object> replyFuture = actorContext.executeOperationAsync(getActor(),
                isTxActorLocal ? readyTransaction : readyTransaction.toSerializable());

            // Combine all the previously recorded put/merge/delete operation reply Futures and the
            // ReadyTransactionReply Future into one Future. If any one fails then the combined
            // Future will fail. We need all prior operations and the ready operation to succeed
            // in order to attempt commit.

            List<Future<Object>> futureList =
                    Lists.newArrayListWithCapacity(recordedOperationFutures.size() + 1);
            futureList.addAll(recordedOperationFutures);
            futureList.add(replyFuture);

            Future<Iterable<Object>> combinedFutures = akka.dispatch.Futures.sequence(futureList,
                    actorContext.getActorSystem().dispatcher());

            // Transform the combined Future into a Future that returns the cohort actor path from
            // the ReadyTransactionReply. That's the end result of the ready operation.

            return combinedFutures.transform(new Mapper<Iterable<Object>, ActorSelection>() {
                @Override
                public ActorSelection checkedApply(Iterable<Object> notUsed) {
                    LOG.debug("Tx {} readyTransaction: pending recorded operations succeeded",
                            identifier);

                    // At this point all the Futures succeeded and we need to extract the cohort
                    // actor path from the ReadyTransactionReply. For the recorded operations, they
                    // don't return any data so we're only interested that they completed
                    // successfully. We could be paranoid and verify the correct reply types but
                    // that really should never happen so it's not worth the overhead of
                    // de-serializing each reply.

                    // Note the Future get call here won't block as it's complete.
                    Object serializedReadyReply = replyFuture.value().get().get();
                    if (serializedReadyReply instanceof ReadyTransactionReply) {
                        return actorContext.actorSelection(((ReadyTransactionReply)serializedReadyReply).getCohortPath());

                    } else if(serializedReadyReply.getClass().equals(ReadyTransactionReply.SERIALIZABLE_CLASS)) {
                        ReadyTransactionReply reply = ReadyTransactionReply.fromSerializable(serializedReadyReply);
                        String cohortPath = reply.getCohortPath();

                        // In Helium we used to return the local path of the actor which represented
                        // a remote ThreePhaseCommitCohort. The local path would then be converted to
                        // a remote path using this resolvePath method. To maintain compatibility with
                        // a Helium node we need to continue to do this conversion.
                        // At some point in the future when upgrades from Helium are not supported
                        // we could remove this code to resolvePath and just use the cohortPath as the
                        // resolved cohortPath
                        if(TransactionContextImpl.this.remoteTransactionVersion < CreateTransaction.HELIUM_1_VERSION) {
                            cohortPath = actorContext.resolvePath(transactionPath, cohortPath);
                        }

                        return actorContext.actorSelection(cohortPath);

                    } else {
                        // Throwing an exception here will fail the Future.
                        throw new IllegalArgumentException(String.format("Invalid reply type {}",
                                serializedReadyReply.getClass()));
                    }
                }
            }, SAME_FAILURE_TRANSFORMER, actorContext.getActorSystem().dispatcher());
        }

        @Override
        public void deleteData(YangInstanceIdentifier path) {
            LOG.debug("Tx {} deleteData called path = {}", identifier, path);

            DeleteData deleteData = new DeleteData(path);
            recordedOperationFutures.add(actorContext.executeOperationAsync(getActor(),
                isTxActorLocal ? deleteData : deleteData.toSerializable()));
        }

        @Override
        public void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
            LOG.debug("Tx {} mergeData called path = {}", identifier, path);

            MergeData mergeData = new MergeData(path, data, schemaContext);
            recordedOperationFutures.add(actorContext.executeOperationAsync(getActor(),
                isTxActorLocal ? mergeData : mergeData.toSerializable()));
        }

        @Override
        public void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
            LOG.debug("Tx {} writeData called path = {}", identifier, path);

            WriteData writeData = new WriteData(path, data, schemaContext);
            recordedOperationFutures.add(actorContext.executeOperationAsync(getActor(),
                isTxActorLocal ? writeData : writeData.toSerializable()));
        }

        @Override
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readData(
                final YangInstanceIdentifier path) {

            LOG.debug("Tx {} readData called path = {}", identifier, path);

            final SettableFuture<Optional<NormalizedNode<?, ?>>> returnFuture = SettableFuture.create();

            // If there were any previous recorded put/merge/delete operation reply Futures then we
            // must wait for them to successfully complete. This is necessary to honor the read
            // uncommitted semantics of the public API contract. If any one fails then fail the read.

            if(recordedOperationFutures.isEmpty()) {
                finishReadData(path, returnFuture);
            } else {
                LOG.debug("Tx {} readData: verifying {} previous recorded operations",
                        identifier, recordedOperationFutures.size());

                // Note: we make a copy of recordedOperationFutures to be on the safe side in case
                // Futures#sequence accesses the passed List on a different thread, as
                // recordedOperationFutures is not synchronized.

                Future<Iterable<Object>> combinedFutures = akka.dispatch.Futures.sequence(
                        Lists.newArrayList(recordedOperationFutures),
                        actorContext.getActorSystem().dispatcher());

                OnComplete<Iterable<Object>> onComplete = new OnComplete<Iterable<Object>>() {
                    @Override
                    public void onComplete(Throwable failure, Iterable<Object> notUsed)
                            throws Throwable {
                        if(failure != null) {
                            LOG.debug("Tx {} readData: a recorded operation failed: {}",
                                    identifier, failure);
                            returnFuture.setException(new ReadFailedException(
                                    "The read could not be performed because a previous put, merge,"
                                    + "or delete operation failed", failure));
                        } else {
                            finishReadData(path, returnFuture);
                        }
                    }
                };

                combinedFutures.onComplete(onComplete, actorContext.getActorSystem().dispatcher());
            }

            return MappingCheckedFuture.create(returnFuture, ReadFailedException.MAPPER);
        }

        private void finishReadData(final YangInstanceIdentifier path,
                final SettableFuture<Optional<NormalizedNode<?, ?>>> returnFuture) {

            LOG.debug("Tx {} finishReadData called path = {}", identifier, path);

            OnComplete<Object> onComplete = new OnComplete<Object>() {
                @Override
                public void onComplete(Throwable failure, Object readResponse) throws Throwable {
                    if(failure != null) {
                        LOG.debug("Tx {} read operation failed: {}", identifier, failure);
                        returnFuture.setException(new ReadFailedException(
                                "Error reading data for path " + path, failure));

                    } else {
                        LOG.debug("Tx {} read operation succeeded", identifier, failure);

                        if (readResponse instanceof ReadDataReply) {
                            ReadDataReply reply = (ReadDataReply) readResponse;
                            returnFuture.set(Optional.<NormalizedNode<?, ?>>fromNullable(reply.getNormalizedNode()));

                        } else if (readResponse.getClass().equals(ReadDataReply.SERIALIZABLE_CLASS)) {
                            ReadDataReply reply = ReadDataReply.fromSerializable(schemaContext, path, readResponse);
                            returnFuture.set(Optional.<NormalizedNode<?, ?>>fromNullable(reply.getNormalizedNode()));

                        } else {
                            returnFuture.setException(new ReadFailedException(
                                "Invalid response reading data for path " + path));
                        }
                    }
                }
            };

            ReadData readData = new ReadData(path);
            Future<Object> readFuture = actorContext.executeOperationAsync(getActor(),
                isTxActorLocal ? readData : readData.toSerializable());

            readFuture.onComplete(onComplete, actorContext.getActorSystem().dispatcher());
        }

        @Override
        public CheckedFuture<Boolean, ReadFailedException> dataExists(
                final YangInstanceIdentifier path) {

            LOG.debug("Tx {} dataExists called path = {}", identifier, path);

            final SettableFuture<Boolean> returnFuture = SettableFuture.create();

            // If there were any previous recorded put/merge/delete operation reply Futures then we
            // must wait for them to successfully complete. This is necessary to honor the read
            // uncommitted semantics of the public API contract. If any one fails then fail this
            // request.

            if(recordedOperationFutures.isEmpty()) {
                finishDataExists(path, returnFuture);
            } else {
                LOG.debug("Tx {} dataExists: verifying {} previous recorded operations",
                        identifier, recordedOperationFutures.size());

                // Note: we make a copy of recordedOperationFutures to be on the safe side in case
                // Futures#sequence accesses the passed List on a different thread, as
                // recordedOperationFutures is not synchronized.

                Future<Iterable<Object>> combinedFutures = akka.dispatch.Futures.sequence(
                        Lists.newArrayList(recordedOperationFutures),
                        actorContext.getActorSystem().dispatcher());
                OnComplete<Iterable<Object>> onComplete = new OnComplete<Iterable<Object>>() {
                    @Override
                    public void onComplete(Throwable failure, Iterable<Object> notUsed)
                            throws Throwable {
                        if(failure != null) {
                            LOG.debug("Tx {} dataExists: a recorded operation failed: {}",
                                    identifier, failure);
                            returnFuture.setException(new ReadFailedException(
                                    "The data exists could not be performed because a previous "
                                    + "put, merge, or delete operation failed", failure));
                        } else {
                            finishDataExists(path, returnFuture);
                        }
                    }
                };

                combinedFutures.onComplete(onComplete, actorContext.getActorSystem().dispatcher());
            }

            return MappingCheckedFuture.create(returnFuture, ReadFailedException.MAPPER);
        }

        private void finishDataExists(final YangInstanceIdentifier path,
                final SettableFuture<Boolean> returnFuture) {

            LOG.debug("Tx {} finishDataExists called path = {}", identifier, path);

            OnComplete<Object> onComplete = new OnComplete<Object>() {
                @Override
                public void onComplete(Throwable failure, Object response) throws Throwable {
                    if(failure != null) {
                        LOG.debug("Tx {} dataExists operation failed: {}", identifier, failure);
                        returnFuture.setException(new ReadFailedException(
                                "Error checking data exists for path " + path, failure));
                    } else {
                        LOG.debug("Tx {} dataExists operation succeeded", identifier, failure);

                        if (response instanceof DataExistsReply) {
                            returnFuture.set(Boolean.valueOf(((DataExistsReply) response).exists()));

                        } else if (response.getClass().equals(DataExistsReply.SERIALIZABLE_CLASS)) {
                            returnFuture.set(Boolean.valueOf(DataExistsReply.fromSerializable(response).exists()));

                        } else {
                            returnFuture.setException(new ReadFailedException(
                                    "Invalid response checking exists for path " + path));
                        }
                    }
                }
            };

            DataExists dataExists = new DataExists(path);
            Future<Object> future = actorContext.executeOperationAsync(getActor(),
                isTxActorLocal ? dataExists : dataExists.toSerializable());

            future.onComplete(onComplete, actorContext.getActorSystem().dispatcher());
        }
    }

    private static class NoOpTransactionContext extends AbstractTransactionContext {

        private final Logger LOG = LoggerFactory.getLogger(NoOpTransactionContext.class);

        private final Throwable failure;

        public NoOpTransactionContext(Throwable failure, TransactionIdentifier identifier){
            super(identifier);
            this.failure = failure;
        }

        @Override
        public void closeTransaction() {
            LOG.debug("NoOpTransactionContext {} closeTransaction called", identifier);
        }

        @Override
        public Future<ActorSelection> readyTransaction() {
            LOG.debug("Tx {} readyTransaction called", identifier);
            return akka.dispatch.Futures.failed(failure);
        }

        @Override
        public void deleteData(YangInstanceIdentifier path) {
            LOG.debug("Tx {} deleteData called path = {}", identifier, path);
        }

        @Override
        public void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
            LOG.debug("Tx {} mergeData called path = {}", identifier, path);
        }

        @Override
        public void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
            LOG.debug("Tx {} writeData called path = {}", identifier, path);
        }

        @Override
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readData(
                YangInstanceIdentifier path) {
            LOG.debug("Tx {} readData called path = {}", identifier, path);
            return Futures.immediateFailedCheckedFuture(new ReadFailedException(
                    "Error reading data for path " + path, failure));
        }

        @Override
        public CheckedFuture<Boolean, ReadFailedException> dataExists(
                YangInstanceIdentifier path) {
            LOG.debug("Tx {} dataExists called path = {}", identifier, path);
            return Futures.immediateFailedCheckedFuture(new ReadFailedException(
                    "Error checking exists for path " + path, failure));
        }
    }
}
