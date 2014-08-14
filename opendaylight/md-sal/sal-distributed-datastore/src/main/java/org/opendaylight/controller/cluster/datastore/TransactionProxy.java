/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.opendaylight.controller.cluster.datastore.exceptions.OperationTimeoutException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.RemoteOperationException;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TransactionProxy acts as a proxy for one or more transactions that were created on a remote shard
 * <p>
 * Creating a transaction on the consumer side will create one instance of a transaction proxy. If during
 * the transaction reads and writes are done on data that belongs to different shards then a separate transaction will
 * be created on each of those shards by the TransactionProxy
 * </p>
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

    private static final AtomicLong counter = new AtomicLong();

    private static final Logger
        LOG = LoggerFactory.getLogger(TransactionProxy.class);


    private final TransactionType transactionType;
    private final ActorContext actorContext;
    private final Map<String, TransactionContext> remoteTransactionPaths =
        new HashMap<>();
    private final TransactionIdentifier identifier;
    private final ListeningExecutorService executor;
    private final SchemaContext schemaContext;
    private boolean inReadyState;
    public TransactionProxy(
        ActorContext actorContext,
        TransactionType transactionType,
        ListeningExecutorService executor,
        SchemaContext schemaContext
    ) {
        this.actorContext = Preconditions.checkNotNull(actorContext, "actorContext should not be null");
        this.transactionType = Preconditions.checkNotNull(transactionType,"transactionType should not be null");
        this.executor =Preconditions.checkNotNull(executor, "executor should not be null");
        this.schemaContext = Preconditions.checkNotNull(schemaContext, "schemaContext should not be null");

        String memberName = actorContext.getCurrentMemberName();
        if (memberName == null) {
            memberName = "UNKNOWN-MEMBER";
        }
        this.identifier = TransactionIdentifier.builder().memberName(memberName).counter(counter.getAndIncrement()).build();

        LOG.debug("Created txn {}", identifier);

    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final YangInstanceIdentifier path) {

        Preconditions.checkState(transactionType != TransactionType.WRITE_ONLY,"read operation on write-only transaction not allowed");

        LOG.debug("txn {} read {}", identifier, path);

        createTransactionIfMissing(actorContext, path);

        return transactionContext(path).readData(path);
    }

    @Override public CheckedFuture<Boolean, ReadFailedException> exists(YangInstanceIdentifier path) {
        LOG.debug("txn {} exists {}", identifier, path);

        createTransactionIfMissing(actorContext, path);

        return transactionContext(path).dataExists(path);
    }

    @Override
    public void write(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {

        Preconditions.checkState(transactionType != TransactionType.READ_ONLY,
            "write operation on read-only transaction");
        Preconditions.checkState(inReadyState == false,
            "modification not allowed when cohort is in ready state");

        LOG.debug("txn {} write {}", identifier, path);

        createTransactionIfMissing(actorContext, path);


        transactionContext(path).writeData(path, data);
    }

    @Override
    public void merge(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        Preconditions.checkState(transactionType != TransactionType.READ_ONLY,"merge operation on read-only transaction");
        Preconditions.checkState(inReadyState == false, "modification not allowed when cohort is in ready state");

        LOG.debug("txn {} merge {}", identifier, path);

        createTransactionIfMissing(actorContext, path);

        transactionContext(path).mergeData(path, data);
    }

    @Override
    public void delete(YangInstanceIdentifier path) {
        Preconditions.checkState(transactionType != TransactionType.READ_ONLY,"delete operation on read-only transaction");
        Preconditions.checkState(inReadyState == false,"modification not allowed when cohort is in ready state");
        LOG.debug("txn {} delete {}", identifier, path);

        createTransactionIfMissing(actorContext, path);

        transactionContext(path).deleteData(path);
    }

    @Override
    public DOMStoreThreePhaseCommitCohort ready() {
        List<ActorPath> cohortPaths = new ArrayList<>();

        LOG.debug("txn {} Trying to get {} transactions ready for commit", identifier,
            remoteTransactionPaths.size());

        for (TransactionContext transactionContext : remoteTransactionPaths.values()) {

            LOG.debug("txn {} Readying transaction for shard {}", identifier,
                transactionContext.getShardName());

            Object result = transactionContext.readyTransaction();

            if (result.getClass().equals(ReadyTransactionReply.SERIALIZABLE_CLASS)) {
                ReadyTransactionReply reply = ReadyTransactionReply.fromSerializable(actorContext.getActorSystem(), result);
                String resolvedCohortPath = transactionContext.getResolvedCohortPath(reply.getCohortPath().toString());
                cohortPaths.add(actorContext.actorFor(resolvedCohortPath));
            }else{
                LOG.error("Was expecting {} but got {}",ReadyTransactionReply.SERIALIZABLE_CLASS,result.getClass());
            }
        }
        inReadyState = true;
        return new ThreePhaseCommitCohortProxy(actorContext, cohortPaths,identifier.toString(), executor);
    }

    @Override
    public Object getIdentifier() {
        return this.identifier;
    }

    @Override
    public void close() {
        for (TransactionContext transactionContext : remoteTransactionPaths.values()) {
            transactionContext.closeTransaction();
        }
    }

    private TransactionContext transactionContext(YangInstanceIdentifier path) {
        String shardName = shardNameFromIdentifier(path);
        return remoteTransactionPaths.get(shardName);
    }

    private String shardNameFromIdentifier(YangInstanceIdentifier path) {
        return ShardStrategyFactory.getStrategy(path).findShard(path);
    }

    private void createTransactionIfMissing(ActorContext actorContext,
        YangInstanceIdentifier path) {
        String shardName = ShardStrategyFactory.getStrategy(path).findShard(path);

        TransactionContext transactionContext = remoteTransactionPaths.get(shardName);

        if (transactionContext != null) {
            // A transaction already exists with that shard
            return;
        }

        try {
            Object response = actorContext.executeShardOperation(shardName,
                new CreateTransaction(identifier.toString(),this.transactionType.ordinal()).toSerializable(),
                ActorContext.ASK_DURATION);
            if (response.getClass().equals(CreateTransactionReply.SERIALIZABLE_CLASS)) {
                CreateTransactionReply reply = CreateTransactionReply.fromSerializable(response);

                String transactionPath = reply.getTransactionPath();

                LOG.debug("txn {} Received transaction path = {}", identifier,
                    transactionPath);

                ActorSelection transactionActor = actorContext.actorSelection(transactionPath);
                transactionContext = new TransactionContextImpl(shardName, transactionPath,
                        transactionActor);

                remoteTransactionPaths.put(shardName, transactionContext);
            }else{
                LOG.error("{} was expected but got {}",CreateTransactionReply.SERIALIZABLE_CLASS,response.getClass());
            }
        } catch (OperationTimeoutException | PrimaryNotFoundException e) {

            LOG.error("txn {} Creating NoOpTransaction because of : {}", identifier, e.getMessage());
            remoteTransactionPaths.put(shardName, new NoOpTransactionContext(shardName));
        } catch (org.opendaylight.controller.cluster.datastore.exceptions.RemoteOperationException e) {
            //throwing IllegalStateException as some runtime scenario failure
            //during transaction creation
            throw new IllegalStateException("Creating txn with" + identifier + " failed", e);

        }
    }

    private interface TransactionContext {
        String getShardName();

        String getResolvedCohortPath(String cohortPath);

        public void closeTransaction();

        public Object readyTransaction();

        void deleteData(YangInstanceIdentifier path);

        void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data);

        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readData(
            final YangInstanceIdentifier path);

        void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data);

        CheckedFuture<Boolean, ReadFailedException> dataExists(
            YangInstanceIdentifier path);
    }


    private class TransactionContextImpl implements TransactionContext {
        private final String shardName;
        private final String actorPath;
        private final ActorSelection actor;


        private TransactionContextImpl(String shardName, String actorPath,
            ActorSelection actor) {
            this.shardName = shardName;
            this.actorPath = actorPath;
            this.actor = actor;
        }

        @Override public String getShardName() {
            return shardName;
        }

        private ActorSelection getActor() {
            return actor;
        }

        @Override public String getResolvedCohortPath(String cohortPath) {
            return actorContext.resolvePath(actorPath, cohortPath);
        }

        @Override public void closeTransaction() {
            getActor().tell(new CloseTransaction().toSerializable(), null);
        }

        @Override public Object readyTransaction() {
            try {
                return actorContext.executeRemoteOperation(getActor(),new ReadyTransaction().toSerializable(),
                    ActorContext.ASK_DURATION
                );
            } catch (RemoteOperationException roe) {
                throw new IllegalStateException(roe);
            }
        }

        @Override public void deleteData(YangInstanceIdentifier path) {

            try {
                actorContext.executeRemoteOperation(getActor(),new DeleteData(path).toSerializable(),
                        ActorContext.AWAIT_MODIFY_DURATION);

            } catch (Exception e) {
                throw new IllegalStateException("Failed to delete data at path " + path, e);
            }
        }

        @Override public void mergeData(YangInstanceIdentifier path,
            NormalizedNode<?, ?> data) {
            try {
                actorContext.executeRemoteOperation(getActor(),
                        new MergeData(path, data, schemaContext)
                            .toSerializable(),
                        ActorContext.AWAIT_MODIFY_DURATION);

            } catch (Exception e) {
                throw new IllegalStateException("Failed to merge data at path " + path, e);
            }
        }

        @Override
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readData(
            final YangInstanceIdentifier path) {

            Callable<Optional<NormalizedNode<?, ?>>> call =
                new Callable<Optional<NormalizedNode<?, ?>>>() {

                    @Override public Optional<NormalizedNode<?, ?>> call() throws Exception {
                        Object response = actorContext.executeRemoteOperation(getActor(),
                                new ReadData(path).toSerializable(),
                                ActorContext.ASK_DURATION);
                        if (response.getClass().equals(ReadDataReply.SERIALIZABLE_CLASS)) {
                            ReadDataReply reply = ReadDataReply.fromSerializable(schemaContext, path,
                                    response);
                            if (reply.getNormalizedNode() == null) {
                                return Optional.absent();
                            }
                            return Optional.<NormalizedNode<?, ?>>of(
                                reply.getNormalizedNode());
                        }

                        throw new ReadFailedException("Read Failed " + path);
                    }
                };

            return MappingCheckedFuture
                .create(executor.submit(call), ReadFailedException.MAPPER);
        }

        @Override public void writeData(YangInstanceIdentifier path,NormalizedNode<?, ?> data) {
            try {
                actorContext.executeRemoteOperation(getActor(),new WriteData(path, data, schemaContext)
                            .toSerializable(),ActorContext.AWAIT_MODIFY_DURATION);

            } catch (Exception e) {
                throw new IllegalStateException(
                    "Failed to write data at path " + path, e);
            }
        }

        @Override public CheckedFuture<Boolean, ReadFailedException> dataExists(final YangInstanceIdentifier path) {

            Callable<Boolean> call = new Callable<Boolean>() {

                @Override public Boolean call() throws Exception {
                    Object o = actorContext.executeRemoteOperation(getActor(),
                        new DataExists(path).toSerializable(),
                        ActorContext.ASK_DURATION
                    );


                    if (DataExistsReply.SERIALIZABLE_CLASS.equals(o.getClass())) {
                        return DataExistsReply.fromSerializable(o).exists();
                    }

                    throw new ReadFailedException("Exists Failed " + path);
                }
            };
            return MappingCheckedFuture.create(executor.submit(call), ReadFailedException.MAPPER);
        }
    }

    private class NoOpTransactionContext implements TransactionContext {

        private final Logger
            LOG = LoggerFactory.getLogger(NoOpTransactionContext.class);

        private final String shardName;

        private ActorRef cohort;

        public NoOpTransactionContext(String shardName) {
            this.shardName = shardName;
        }

        @Override public String getShardName() {
            return shardName;

        }

        @Override public String getResolvedCohortPath(String cohortPath) {
            return cohort.path().toString();
        }

        @Override public void closeTransaction() {
            LOG.warn("txn {} closeTransaction called", identifier);
        }

        @Override public Object readyTransaction() {
            LOG.warn("txn {} readyTransaction called", identifier);
            cohort = actorContext.getActorSystem().actorOf(Props.create(NoOpCohort.class));
            return new ReadyTransactionReply(cohort.path()).toSerializable();
        }

        @Override public void deleteData(YangInstanceIdentifier path) {
            LOG.warn("txt {} deleteData called path = {}", identifier, path);
        }

        @Override public void mergeData(YangInstanceIdentifier path,
            NormalizedNode<?, ?> data) {
            LOG.warn("txn {} mergeData called path = {}", identifier, path);
        }

        @Override
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readData(
            YangInstanceIdentifier path) {
            LOG.warn("txn {} readData called path = {}", identifier, path);
            return Futures.immediateCheckedFuture(
                Optional.<NormalizedNode<?, ?>>absent());
        }

        @Override public void writeData(YangInstanceIdentifier path,
            NormalizedNode<?, ?> data) {
            LOG.warn("txn {} writeData called path = {}", identifier, path);
        }

        @Override public CheckedFuture<Boolean, ReadFailedException> dataExists(
            YangInstanceIdentifier path) {
            LOG.warn("txn {} dataExists called path = {}", identifier, path);

            // Returning false instead of an exception to keep this aligned with
            // read
            return Futures.immediateCheckedFuture(false);
        }
    }

}
