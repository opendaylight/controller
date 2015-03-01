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
import com.google.common.base.FinalizablePhantomReference;
import com.google.common.base.FinalizableReferenceQueue;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
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
     * Time interval in between transaction create retries.
     */
    private static final FiniteDuration CREATE_TX_TRY_INTERVAL =
            FiniteDuration.create(1, TimeUnit.SECONDS);

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
                    actorContext.sendOperationAsync(actor, CloseTransaction.INSTANCE.toSerializable());
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
    private volatile AtomicBoolean remoteTransactionActorsMB;

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

    private volatile boolean initialized;
    private Semaphore operationLimiter;
    private OperationCompleter operationCompleter;

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

        this.identifier = new TransactionIdentifier(memberName, counter.getAndIncrement());

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
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final YangInstanceIdentifier path) {

        Preconditions.checkState(transactionType != TransactionType.WRITE_ONLY,
                "Read operation on write-only transaction is not allowed");

        LOG.debug("Tx {} read {}", identifier, path);

        throttleOperation();

        final SettableFuture<Optional<NormalizedNode<?, ?>>> proxyFuture = SettableFuture.create();

        TransactionFutureCallback txFutureCallback = getOrCreateTxFutureCallback(path);
        txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.readData(path, proxyFuture);
            }
        });

        return MappingCheckedFuture.create(proxyFuture, ReadFailedException.MAPPER);
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {

        Preconditions.checkState(transactionType != TransactionType.WRITE_ONLY,
                "Exists operation on write-only transaction is not allowed");

        LOG.debug("Tx {} exists {}", identifier, path);

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
        Preconditions.checkState(!inReadyState,
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


    @Override
    public void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {

        checkModificationState();

        LOG.debug("Tx {} write {}", identifier, path);

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

        LOG.debug("Tx {} merge {}", identifier, path);

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

        LOG.debug("Tx {} delete {}", identifier, path);

        throttleOperation();

        TransactionFutureCallback txFutureCallback = getOrCreateTxFutureCallback(path);
        txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.deleteData(path);
            }
        });
    }

    @Override
    public DOMStoreThreePhaseCommitCohort ready() {

        checkModificationState();

        inReadyState = true;

        LOG.debug("Tx {} Readying {} transactions for commit", identifier,
                    txFutureCallbackMap.size());

        if(txFutureCallbackMap.size() == 0) {
            onTransactionReady(Collections.<Future<ActorSelection>>emptyList());
            return NoOpDOMStoreThreePhaseCommitCohort.INSTANCE;
        }

        throttleOperation(txFutureCallbackMap.size());

        List<Future<ActorSelection>> cohortFutures = Lists.newArrayList();

        for(TransactionFutureCallback txFutureCallback : txFutureCallbackMap.values()) {

            LOG.debug("Tx {} Readying transaction for shard {} chain {}", identifier,
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
        return actorContext.executeOperationAsync(shard, serializedCreateMessage);
    }

    @Override
    public Object getIdentifier() {
        return this.identifier;
    }

    @Override
    public void close() {
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
            }, actorContext.getClientDispatcher());
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
     * Interfaces for transaction operations to be invoked later.
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

        private volatile int createTxTries = (int) (actorContext.getDatastoreContext().
                getShardLeaderElectionTimeout().duration().toMillis() /
                CREATE_TX_TRY_INTERVAL.toMillis());

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
            boolean invokeOperation = true;
            synchronized(txOperationsOnComplete) {
                if(transactionContext == null) {
                    LOG.debug("Tx {} Adding operation on complete {}", identifier);

                    invokeOperation = false;
                    txOperationsOnComplete.add(operation);
                }
            }

            if(invokeOperation) {
                operation.invoke(transactionContext);
            }
        }

        void enqueueTransactionOperation(final TransactionOperation op) {

            if (transactionContext != null) {
                op.invoke(transactionContext);
            } else {
                // The shard Tx hasn't been created yet so add the Tx operation to the Tx Future
                // callback to be executed after the Tx is created.
                addTxOperationOnComplete(op);
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

            createTxFuture.onComplete(this, actorContext.getClientDispatcher());
        }

        @Override
        public void onComplete(Throwable failure, Object response) {
            if(failure instanceof NoShardLeaderException) {
                // There's no leader for the shard yet - schedule and try again, unless we're out
                // of retries. Note: createTxTries is volatile as it may be written by different
                // threads however not concurrently, therefore decrementing it non-atomically here
                // is ok.
                if(--createTxTries > 0) {
                    LOG.debug("Tx {} Shard {} has no leader yet - scheduling create Tx retry",
                            identifier, shardName);

                    actorContext.getActorSystem().scheduler().scheduleOnce(CREATE_TX_TRY_INTERVAL,
                            new Runnable() {
                                @Override
                                public void run() {
                                    tryCreateTransaction();
                                }
                            }, actorContext.getClientDispatcher());
                    return;
                }
            }

            // Mainly checking for state violation here to perform a volatile read of "initialized" to
            // ensure updates to operationLimter et al are visible to this thread (ie we're doing
            // "piggy-back" synchronization here).
            Preconditions.checkState(initialized, "Tx was not propertly initialized.");

            // Create the TransactionContext from the response or failure. Store the new
            // TransactionContext locally until we've completed invoking the
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

                localTransactionContext = new NoOpTransactionContext(failure, identifier, operationLimiter);
            } else if (response.getClass().equals(CreateTransactionReply.SERIALIZABLE_CLASS)) {
                localTransactionContext = createValidTransactionContext(
                        CreateTransactionReply.fromSerializable(response));
            } else {
                IllegalArgumentException exception = new IllegalArgumentException(String.format(
                        "Invalid reply type %s for CreateTransaction", response.getClass()));

                localTransactionContext = new NoOpTransactionContext(exception, identifier, operationLimiter);
            }

            executeTxOperatonsOnComplete(localTransactionContext);
        }

        private void executeTxOperatonsOnComplete(TransactionContext localTransactionContext) {
            while(true) {
                // Access to txOperationsOnComplete and transactionContext must be protected and atomic
                // (ie synchronized) with respect to #addTxOperationOnComplete to handle timing
                // issues and ensure no TransactionOperation is missed and that they are processed
                // in the order they occurred.

                // We'll make a local copy of the txOperationsOnComplete list to handle re-entrancy
                // in case a TransactionOperation results in another transaction operation being
                // queued (eg a put operation from a client read Future callback that is notified
                // synchronously).
                Collection<TransactionOperation> operationsBatch = null;
                synchronized(txOperationsOnComplete) {
                    if(txOperationsOnComplete.isEmpty()) {
                        // We're done invoking the TransactionOperations so we can now publish the
                        // TransactionContext.
                        transactionContext = localTransactionContext;
                        break;
                    }

                    operationsBatch = new ArrayList<>(txOperationsOnComplete);
                    txOperationsOnComplete.clear();
                }

                // Invoke TransactionOperations outside the sync block to avoid unnecessary blocking.
                // A slight down-side is that we need to re-acquire the lock below but this should
                // be negligible.
                for(TransactionOperation oper: operationsBatch) {
                    oper.invoke(localTransactionContext);
                }
            }
        }

        private TransactionContext createValidTransactionContext(CreateTransactionReply reply) {
            String transactionPath = reply.getTransactionPath();

            LOG.debug("Tx {} Received {}", identifier, reply);

            ActorSelection transactionActor = actorContext.actorSelection(transactionPath);

            if (transactionType == TransactionType.READ_ONLY) {
                // Read-only Tx's aren't explicitly closed by the client so we create a PhantomReference
                // to close the remote Tx's when this instance is no longer in use and is garbage
                // collected.

                if(remoteTransactionActorsMB == null) {
                    remoteTransactionActors = Lists.newArrayList();
                    remoteTransactionActorsMB = new AtomicBoolean();

                    TransactionProxyCleanupPhantomReference cleanup =
                            new TransactionProxyCleanupPhantomReference(TransactionProxy.this);
                    phantomReferenceCache.put(cleanup, cleanup);
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

            if(reply.getVersion() >= DataStoreVersions.LITHIUM_VERSION) {
                return new TransactionContextImpl(transactionPath, transactionActor, identifier,
                    actorContext, schemaContext, isTxActorLocal, reply.getVersion(), operationCompleter);
            } else {
                return new LegacyTransactionContextImpl(transactionPath, transactionActor, identifier,
                        actorContext, schemaContext, isTxActorLocal, reply.getVersion(), operationCompleter);
            }
        }
    }

    private static class NoOpDOMStoreThreePhaseCommitCohort implements DOMStoreThreePhaseCommitCohort {
        static NoOpDOMStoreThreePhaseCommitCohort INSTANCE = new NoOpDOMStoreThreePhaseCommitCohort();

        private static final ListenableFuture<Void> IMMEDIATE_VOID_SUCCESS =
                com.google.common.util.concurrent.Futures.immediateFuture(null);
        private static final ListenableFuture<Boolean> IMMEDIATE_BOOLEAN_SUCCESS =
                com.google.common.util.concurrent.Futures.immediateFuture(Boolean.TRUE);

        private NoOpDOMStoreThreePhaseCommitCohort() {
        }

        @Override
        public ListenableFuture<Boolean> canCommit() {
            return IMMEDIATE_BOOLEAN_SUCCESS;
        }

        @Override
        public ListenableFuture<Void> preCommit() {
            return IMMEDIATE_VOID_SUCCESS;
        }

        @Override
        public ListenableFuture<Void> abort() {
            return IMMEDIATE_VOID_SUCCESS;
        }

        @Override
        public ListenableFuture<Void> commit() {
            return IMMEDIATE_VOID_SUCCESS;
        }
    }
}
