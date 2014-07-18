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
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

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

        this.identifier = "txn-" + counter.getAndIncrement();
        this.transactionType = transactionType;
        this.actorContext = actorContext;
        this.executor = executor;
        this.schemaContext = schemaContext;


    }

    @Override
    public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final InstanceIdentifier path) {

        createTransactionIfMissing(actorContext, path);

        final ActorSelection remoteTransaction = remoteTransactionFromIdentifier(path);

        Callable<Optional<NormalizedNode<?,?>>> call = new Callable() {

            @Override public Optional<NormalizedNode<?,?>> call() throws Exception {
                Object response = actorContext
                    .executeRemoteOperation(remoteTransaction, new ReadData(path).toSerializable(),
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

    @Override
    public void write(InstanceIdentifier path, NormalizedNode<?, ?> data) {

        createTransactionIfMissing(actorContext, path);

        final ActorSelection remoteTransaction = remoteTransactionFromIdentifier(path);
        remoteTransaction.tell(new WriteData(path, data, schemaContext).toSerializable(), null);
    }

    @Override
    public void merge(InstanceIdentifier path, NormalizedNode<?, ?> data) {

        createTransactionIfMissing(actorContext, path);

        final ActorSelection remoteTransaction = remoteTransactionFromIdentifier(path);
        remoteTransaction.tell(new MergeData(path, data, schemaContext).toSerializable(), null);
    }

    @Override
    public void delete(InstanceIdentifier path) {

        createTransactionIfMissing(actorContext, path);

        final ActorSelection remoteTransaction = remoteTransactionFromIdentifier(path);
        remoteTransaction.tell(new DeleteData(path).toSerializable(), null);
    }

    @Override
    public DOMStoreThreePhaseCommitCohort ready() {
        List<ActorPath> cohortPaths = new ArrayList<>();

        for(TransactionContext transactionContext : remoteTransactionPaths.values()) {
            Object result = actorContext.executeRemoteOperation(transactionContext.getActor(),
                new ReadyTransaction().toSerializable(),
                ActorContext.ASK_DURATION
            );

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
            transactionContext.getActor().tell(
                new CloseTransaction().toSerializable(), null);
        }
    }

    private ActorSelection remoteTransactionFromIdentifier(InstanceIdentifier path){
        String shardName = shardNameFromIdentifier(path);
        return remoteTransactionPaths.get(shardName).getActor();
    }

    private String shardNameFromIdentifier(InstanceIdentifier path){
        return ShardStrategyFactory.getStrategy(path).findShard(path);
    }

    private void createTransactionIfMissing(ActorContext actorContext, InstanceIdentifier path) {
        String shardName = ShardStrategyFactory.getStrategy(path).findShard(path);

        TransactionContext transactionContext =
            remoteTransactionPaths.get(shardName);

        if(transactionContext != null){
            // A transaction already exists with that shard
            return;
        }

        Object response = actorContext.executeShardOperation(shardName, new CreateTransaction(identifier).toSerializable(), ActorContext.ASK_DURATION);
        if(response.getClass().equals(CreateTransactionReply.SERIALIZABLE_CLASS)){
            CreateTransactionReply reply = CreateTransactionReply.fromSerializable(response);
            String transactionPath = actorContext.getRemoteActorPath(shardName, reply.getTransactionPath());

            ActorSelection transactionActor = actorContext.actorSelection(transactionPath);
            transactionContext = new TransactionContext(shardName, transactionPath, transactionActor);

            remoteTransactionPaths.put(shardName, transactionContext);
        }
    }


    private class TransactionContext {
        private final String shardName;
        private final String actorPath;
        private final ActorSelection  actor;


        private TransactionContext(String shardName, String actorPath,
            ActorSelection actor) {
            this.shardName = shardName;
            this.actorPath = actorPath;
            this.actor = actor;
        }


        public String getShardName() {
            return shardName;
        }

        public String getActorPath() {
            return actorPath;
        }

        public ActorSelection getActor() {
            return actor;
        }

        public String getResolvedCohortPath(String cohortPath){
            return actorContext.resolvePath(actorPath, cohortPath);
        }
    }

}
