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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
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
import java.util.concurrent.ExecutorService;
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

    private static final AtomicLong counter = new AtomicLong();

    private static final Logger
        LOG = LoggerFactory.getLogger(TransactionProxy.class);


    private final TransactionType transactionType;
    private final ActorContext actorContext;
    private final Map<String, TransactionContext> remoteTransactionPaths = new HashMap<>();
    private final String identifier;
    private final ExecutorService executor;
    private final SchemaContext schemaContext;

    public TransactionProxy(
        ActorContext actorContext,
        TransactionType transactionType,
        ExecutorService executor,
        SchemaContext schemaContext
    ) {

        this.identifier = actorContext.getCurrentMemberName() + "-txn-" + counter.getAndIncrement();
        this.transactionType = transactionType;
        this.actorContext = actorContext;
        this.executor = executor;
        this.schemaContext = schemaContext;


    }

    @Override
    public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final YangInstanceIdentifier path) {

        createTransactionIfMissing(actorContext, path);

        return transactionContext(path).readData(path);
    }

    @Override
    public void write(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {

        createTransactionIfMissing(actorContext, path);

        transactionContext(path).writeData(path, data);
    }

    @Override
    public void merge(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {

        createTransactionIfMissing(actorContext, path);

        transactionContext(path).mergeData(path, data);
    }

    @Override
    public void delete(YangInstanceIdentifier path) {

        createTransactionIfMissing(actorContext, path);

        transactionContext(path).deleteData(path);
    }

    @Override
    public DOMStoreThreePhaseCommitCohort ready() {
        List<ActorPath> cohortPaths = new ArrayList<>();

        for(TransactionContext transactionContext : remoteTransactionPaths.values()) {
            Object result = transactionContext.readyTransaction();

            if(result.getClass().equals(ReadyTransactionReply.SERIALIZABLE_CLASS)){
                ReadyTransactionReply reply = ReadyTransactionReply.fromSerializable(actorContext.getActorSystem(),result);
                String resolvedCohortPath = transactionContext
                    .getResolvedCohortPath(reply.getCohortPath().toString());
                cohortPaths.add(actorContext.actorFor(resolvedCohortPath));
            }
        }

        return new ThreePhaseCommitCohortProxy(actorContext, cohortPaths, identifier, executor);
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
                new CreateTransaction(identifier).toSerializable(),
                ActorContext.ASK_DURATION);
            if (response.getClass()
                .equals(CreateTransactionReply.SERIALIZABLE_CLASS)) {
                CreateTransactionReply reply =
                    CreateTransactionReply.fromSerializable(response);

                String transactionPath = reply.getTransactionPath();

                LOG.info("Received transaction path = {}"  , transactionPath );

                ActorSelection transactionActor =
                    actorContext.actorSelection(transactionPath);
                transactionContext =
                    new TransactionContextImpl(shardName, transactionPath,
                        transactionActor);

                remoteTransactionPaths.put(shardName, transactionContext);
            }
        } catch(TimeoutException e){
            LOG.error("Creating NoOpTransaction because of : {}", e.getMessage());
            remoteTransactionPaths.put(shardName, new NoOpTransactionContext(shardName));
        }
    }

    private interface TransactionContext {
        String getShardName();

        String getResolvedCohortPath(String cohortPath);

        public void closeTransaction();

        public Object readyTransaction();

        void deleteData(YangInstanceIdentifier path);

        void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data);

        ListenableFuture<Optional<NormalizedNode<?, ?>>> readData(final YangInstanceIdentifier path);

        void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data);
    }


    private class TransactionContextImpl implements TransactionContext{
        private final String shardName;
        private final String actorPath;
        private final ActorSelection  actor;


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

        @Override public String getResolvedCohortPath(String cohortPath){
            return actorContext.resolvePath(actorPath, cohortPath);
        }

        @Override public void closeTransaction() {
            getActor().tell(
                new CloseTransaction().toSerializable(), null);
        }

        @Override public Object readyTransaction() {
            return actorContext.executeRemoteOperation(getActor(),
                new ReadyTransaction().toSerializable(),
                ActorContext.ASK_DURATION
            );

        }

        @Override public void deleteData(YangInstanceIdentifier path) {
            getActor().tell(new DeleteData(path).toSerializable(), null);
        }

        @Override public void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data){
            getActor().tell(new MergeData(path, data, schemaContext).toSerializable(), null);
        }

        @Override public ListenableFuture<Optional<NormalizedNode<?, ?>>> readData(final YangInstanceIdentifier path) {

            Callable<Optional<NormalizedNode<?,?>>> call = new Callable() {

                @Override public Optional<NormalizedNode<?,?>> call() throws Exception {
                    Object response = actorContext
                        .executeRemoteOperation(getActor(), new ReadData(path).toSerializable(),
                            ActorContext.ASK_DURATION);
                    if(response.getClass().equals(ReadDataReply.SERIALIZABLE_CLASS)){
                        ReadDataReply reply = ReadDataReply.fromSerializable(schemaContext,path, response);
                        if(reply.getNormalizedNode() == null){
                            return Optional.absent();
                        }
                        //FIXME : A cast should not be required here ???
                        return (Optional<NormalizedNode<?, ?>>) Optional.of(reply.getNormalizedNode());
                    }

                    return Optional.absent();
                }
            };

            ListenableFutureTask<Optional<NormalizedNode<?, ?>>>
                future = ListenableFutureTask.create(call);

            executor.submit(future);

            return future;
        }

        @Override public void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
            getActor().tell(new WriteData(path, data, schemaContext).toSerializable(), null);
        }

    }

    private class NoOpTransactionContext implements TransactionContext {

        private final Logger
            LOG = LoggerFactory.getLogger(NoOpTransactionContext.class);

        private final String shardName;

        private ActorRef cohort;

        public NoOpTransactionContext(String shardName){
            this.shardName = shardName;
        }
        @Override public String getShardName() {
            return  shardName;

        }

        @Override public String getResolvedCohortPath(String cohortPath) {
            return cohort.path().toString();
        }

        @Override public void closeTransaction() {
            LOG.error("closeTransaction called");
        }

        @Override public Object readyTransaction() {
            LOG.error("readyTransaction called");
            cohort = actorContext.getActorSystem().actorOf(Props.create(NoOpCohort.class));
            return new ReadyTransactionReply(cohort.path()).toSerializable();
        }

        @Override public void deleteData(YangInstanceIdentifier path) {
            LOG.error("deleteData called path = {}", path);
        }

        @Override public void mergeData(YangInstanceIdentifier path,
            NormalizedNode<?, ?> data) {
            LOG.error("mergeData called path = {}", path);
        }

        @Override
        public ListenableFuture<Optional<NormalizedNode<?, ?>>> readData(
            YangInstanceIdentifier path) {
            LOG.error("readData called path = {}", path);
            return Futures.immediateFuture(
                Optional.<NormalizedNode<?, ?>>absent());
        }

        @Override public void writeData(YangInstanceIdentifier path,
            NormalizedNode<?, ?> data) {
            LOG.error("writeData called path = {}", path);
        }
    }



}
