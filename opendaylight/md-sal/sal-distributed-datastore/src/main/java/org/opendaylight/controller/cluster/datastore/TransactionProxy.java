/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorPath;
import akka.actor.ActorSelection;
import akka.dispatch.OnComplete;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.FinalizablePhantomReference;
import com.google.common.base.FinalizableReferenceQueue;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;

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

import scala.Function1;
import scala.concurrent.Future;
import scala.runtime.AbstractFunction1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
    public enum TransactionType {
        READ_ONLY,
        WRITE_ONLY,
        READ_WRITE
    }

    static Function1<Throwable, Throwable> SAME_FAILURE_TRANSFORMER = new AbstractFunction1<
                                                                          Throwable, Throwable>() {
        @Override
        public Throwable apply(Throwable failure) {
            return failure;
        }
    };

    private static final AtomicLong counter = new AtomicLong();

    private static final Logger
        LOG = LoggerFactory.getLogger(TransactionProxy.class);


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
                    actorContext.sendRemoteOperationAsync(actor,
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

    private final Map<String, TransactionContext> remoteTransactionPaths = new HashMap<>();

    private final TransactionType transactionType;
    private final ActorContext actorContext;
    private final TransactionIdentifier identifier;
    private final SchemaContext schemaContext;
    private boolean inReadyState;

    public TransactionProxy(ActorContext actorContext, TransactionType transactionType) {
        this.actorContext = Preconditions.checkNotNull(actorContext,
                "actorContext should not be null");
        this.transactionType = Preconditions.checkNotNull(transactionType,
                "transactionType should not be null");
        this.schemaContext = Preconditions.checkNotNull(actorContext.getSchemaContext(),
                "schemaContext should not be null");

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

        LOG.debug("Created txn {} of type {}", identifier, transactionType);
    }

    @VisibleForTesting
    List<Future<Object>> getRecordedOperationFutures() {
        List<Future<Object>> recordedOperationFutures = Lists.newArrayList();
        for(TransactionContext transactionContext : remoteTransactionPaths.values()) {
            recordedOperationFutures.addAll(transactionContext.getRecordedOperationFutures());
        }

        return recordedOperationFutures;
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
            final YangInstanceIdentifier path) {

        Preconditions.checkState(transactionType != TransactionType.WRITE_ONLY,
                "Read operation on write-only transaction is not allowed");

        LOG.debug("Tx {} read {}", identifier, path);

        createTransactionIfMissing(actorContext, path);

        return transactionContext(path).readData(path);
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(YangInstanceIdentifier path) {

        Preconditions.checkState(transactionType != TransactionType.WRITE_ONLY,
                "Exists operation on write-only transaction is not allowed");

        LOG.debug("Tx {} exists {}", identifier, path);

        createTransactionIfMissing(actorContext, path);

        return transactionContext(path).dataExists(path);
    }

    private void checkModificationState() {
        Preconditions.checkState(transactionType != TransactionType.READ_ONLY,
                "Modification operation on read-only transaction is not allowed");
        Preconditions.checkState(!inReadyState,
                "Transaction is sealed - further modifications are allowed");
    }

    @Override
    public void write(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {

        checkModificationState();

        LOG.debug("Tx {} write {}", identifier, path);

        createTransactionIfMissing(actorContext, path);

        transactionContext(path).writeData(path, data);
    }

    @Override
    public void merge(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {

        checkModificationState();

        LOG.debug("Tx {} merge {}", identifier, path);

        createTransactionIfMissing(actorContext, path);

        transactionContext(path).mergeData(path, data);
    }

    @Override
    public void delete(YangInstanceIdentifier path) {

        checkModificationState();

        LOG.debug("Tx {} delete {}", identifier, path);

        createTransactionIfMissing(actorContext, path);

        transactionContext(path).deleteData(path);
    }

    @Override
    public DOMStoreThreePhaseCommitCohort ready() {

        checkModificationState();

        inReadyState = true;

        LOG.debug("Tx {} Trying to get {} transactions ready for commit", identifier,
                remoteTransactionPaths.size());

        List<Future<ActorPath>> cohortPathFutures = Lists.newArrayList();

        for(TransactionContext transactionContext : remoteTransactionPaths.values()) {

            LOG.debug("Tx {} Readying transaction for shard {}", identifier,
                    transactionContext.getShardName());

            cohortPathFutures.add(transactionContext.readyTransaction());
        }

        return new ThreePhaseCommitCohortProxy(actorContext, cohortPathFutures,
                identifier.toString());
    }

    @Override
    public Object getIdentifier() {
        return this.identifier;
    }

    @Override
    public void close() {
        for(TransactionContext transactionContext : remoteTransactionPaths.values()) {
            transactionContext.closeTransaction();
        }

        remoteTransactionPaths.clear();

        if(transactionType == TransactionType.READ_ONLY) {
            remoteTransactionActors.clear();
            remoteTransactionActorsMB.set(true);
        }
    }

    private TransactionContext transactionContext(YangInstanceIdentifier path){
        String shardName = shardNameFromIdentifier(path);
        return remoteTransactionPaths.get(shardName);
    }

    private String shardNameFromIdentifier(YangInstanceIdentifier path){
        return ShardStrategyFactory.getStrategy(path).findShard(path);
    }

    private void createTransactionIfMissing(ActorContext actorContext, YangInstanceIdentifier path) {
        String shardName = ShardStrategyFactory.getStrategy(path).findShard(path);

        TransactionContext transactionContext =
            remoteTransactionPaths.get(shardName);

        if(transactionContext != null){
            // A transaction already exists with that shard
            return;
        }

        try {
            Object response = actorContext.executeShardOperation(shardName,
                new CreateTransaction(identifier.toString(),this.transactionType.ordinal() ).toSerializable(),
                ActorContext.ASK_DURATION);
            if (response.getClass().equals(CreateTransactionReply.SERIALIZABLE_CLASS)) {
                CreateTransactionReply reply =
                    CreateTransactionReply.fromSerializable(response);

                String transactionPath = reply.getTransactionPath();

                LOG.debug("Tx {} Received transaction path = {}", identifier, transactionPath);

                ActorSelection transactionActor = actorContext.actorSelection(transactionPath);

                if(transactionType == TransactionType.READ_ONLY) {
                    // Add the actor to the remoteTransactionActors list for access by the
                    // cleanup PhantonReference.
                    remoteTransactionActors.add(transactionActor);

                    // Write to the memory barrier volatile to publish the above update to the
                    // remoteTransactionActors list for thread visibility.
                    remoteTransactionActorsMB.set(true);
                }

                transactionContext = new TransactionContextImpl(shardName, transactionPath,
                        transactionActor, identifier, actorContext, schemaContext);

                remoteTransactionPaths.put(shardName, transactionContext);
            } else {
                throw new IllegalArgumentException(String.format(
                        "Invalid reply type {} for CreateTransaction", response.getClass()));
            }
        } catch(Exception e){
            LOG.debug("Tx {} Creating NoOpTransaction because of : {}", identifier, e.getMessage());
            remoteTransactionPaths.put(shardName, new NoOpTransactionContext(shardName, e, identifier));
        }
    }

    private interface TransactionContext {
        String getShardName();

        void closeTransaction();

        Future<ActorPath> readyTransaction();

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
        protected final String shardName;
        protected final List<Future<Object>> recordedOperationFutures = Lists.newArrayList();

        AbstractTransactionContext(String shardName, TransactionIdentifier identifier) {
            this.shardName = shardName;
            this.identifier = identifier;
        }

        @Override
        public String getShardName() {
            return shardName;
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
        private final String actorPath;
        private final ActorSelection actor;

        private TransactionContextImpl(String shardName, String actorPath,
                ActorSelection actor, TransactionIdentifier identifier, ActorContext actorContext,
                SchemaContext schemaContext) {
            super(shardName, identifier);
            this.actorPath = actorPath;
            this.actor = actor;
            this.actorContext = actorContext;
            this.schemaContext = schemaContext;
        }

        private ActorSelection getActor() {
            return actor;
        }

        private String getResolvedCohortPath(String cohortPath) {
            return actorContext.resolvePath(actorPath, cohortPath);
        }

        @Override
        public void closeTransaction() {
            LOG.debug("Tx {} closeTransaction called", identifier);
            actorContext.sendRemoteOperationAsync(getActor(), new CloseTransaction().toSerializable());
        }

        @Override
        public Future<ActorPath> readyTransaction() {
            LOG.debug("Tx {} readyTransaction called with {} previous recorded operations pending",
                    identifier, recordedOperationFutures.size());

            // Send the ReadyTransaction message to the Tx actor.

            final Future<Object> replyFuture = actorContext.executeRemoteOperationAsync(getActor(),
                    new ReadyTransaction().toSerializable(), ActorContext.ASK_DURATION);

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

            return combinedFutures.transform(new AbstractFunction1<Iterable<Object>, ActorPath>() {
                @Override
                public ActorPath apply(Iterable<Object> notUsed) {

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
                    if(serializedReadyReply.getClass().equals(
                                                     ReadyTransactionReply.SERIALIZABLE_CLASS)) {
                        ReadyTransactionReply reply = ReadyTransactionReply.fromSerializable(
                                actorContext.getActorSystem(), serializedReadyReply);

                        String resolvedCohortPath = getResolvedCohortPath(
                                reply.getCohortPath().toString());

                        LOG.debug("Tx {} readyTransaction: resolved cohort path {}",
                                identifier, resolvedCohortPath);

                        return actorContext.actorFor(resolvedCohortPath);
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
            recordedOperationFutures.add(actorContext.executeRemoteOperationAsync(getActor(),
                    new DeleteData(path).toSerializable(), ActorContext.ASK_DURATION ));
        }

        @Override
        public void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
            LOG.debug("Tx {} mergeData called path = {}", identifier, path);
            recordedOperationFutures.add(actorContext.executeRemoteOperationAsync(getActor(),
                    new MergeData(path, data, schemaContext).toSerializable(),
                    ActorContext.ASK_DURATION));
        }

        @Override
        public void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
            LOG.debug("Tx {} writeData called path = {}", identifier, path);
            recordedOperationFutures.add(actorContext.executeRemoteOperationAsync(getActor(),
                    new WriteData(path, data, schemaContext).toSerializable(),
                    ActorContext.ASK_DURATION));
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

                        if (readResponse.getClass().equals(ReadDataReply.SERIALIZABLE_CLASS)) {
                            ReadDataReply reply = ReadDataReply.fromSerializable(schemaContext,
                                    path, readResponse);
                            if (reply.getNormalizedNode() == null) {
                                returnFuture.set(Optional.<NormalizedNode<?, ?>>absent());
                            } else {
                                returnFuture.set(Optional.<NormalizedNode<?, ?>>of(
                                        reply.getNormalizedNode()));
                            }
                        } else {
                            returnFuture.setException(new ReadFailedException(
                                    "Invalid response reading data for path " + path));
                        }
                    }
                }
            };

            Future<Object> readFuture = actorContext.executeRemoteOperationAsync(getActor(),
                    new ReadData(path).toSerializable(), ActorContext.ASK_DURATION);
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

                        if (response.getClass().equals(DataExistsReply.SERIALIZABLE_CLASS)) {
                            returnFuture.set(Boolean.valueOf(DataExistsReply.
                                        fromSerializable(response).exists()));
                        } else {
                            returnFuture.setException(new ReadFailedException(
                                    "Invalid response checking exists for path " + path));
                        }
                    }
                }
            };

            Future<Object> future = actorContext.executeRemoteOperationAsync(getActor(),
                    new DataExists(path).toSerializable(), ActorContext.ASK_DURATION);
            future.onComplete(onComplete, actorContext.getActorSystem().dispatcher());
        }
    }

    private static class NoOpTransactionContext extends AbstractTransactionContext {

        private final Logger LOG = LoggerFactory.getLogger(NoOpTransactionContext.class);

        private final Exception failure;

        public NoOpTransactionContext(String shardName, Exception failure,
                TransactionIdentifier identifier){
            super(shardName, identifier);
            this.failure = failure;
        }

        @Override
        public void closeTransaction() {
            LOG.debug("NoOpTransactionContext {} closeTransaction called", identifier);
        }

        @Override
        public Future<ActorPath> readyTransaction() {
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
