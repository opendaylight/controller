/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * TransactionProxy acts as a proxy for one or more transactions that were created on a remote shard
 *
 * Creating a transaction on the consumer side will create one instance of a transaction proxy. If during
 * the transaction reads and writes are done on data that belongs to different shards then a separate transaction will
 * be created on each of those shards by the TransactionProxy
 *
 * The TransactionProxy does not make any guarantees about atomicity or order in which the transactions on the various
 * shards will be executed.
 *
 */
public class TransactionProxy implements DOMStoreReadWriteTransaction {

    public enum TransactionType {
        READ_ONLY,
        WRITE_ONLY,
        READ_WRITE
    }

    private final TransactionType readOnly;
    private final ActorContext actorContext;
    private final Map<String, ActorSelection> remoteTransactionPaths = new HashMap<>();

    public TransactionProxy(
        ActorContext actorContext,
        TransactionType readOnly) {

        this.readOnly = readOnly;
        this.actorContext = actorContext;

        Object response = actorContext.executeShardOperation(Shard.DEFAULT_NAME, new CreateTransaction(), ActorContext.ASK_DURATION);
        if(response instanceof CreateTransactionReply){
            CreateTransactionReply reply = (CreateTransactionReply) response;
            remoteTransactionPaths.put(Shard.DEFAULT_NAME, actorContext.actorSelection(reply.getTransactionPath()));
        }
    }

    @Override
    public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final InstanceIdentifier path) {
        final ActorSelection remoteTransaction = remoteTransactionFromIdentifier(path);

        Callable<Optional<NormalizedNode<?,?>>> call = new Callable() {

            @Override public Optional<NormalizedNode<?,?>> call() throws Exception {
                Object response = actorContext
                    .executeRemoteOperation(remoteTransaction, new ReadData(path),
                        ActorContext.ASK_DURATION);
                if(response instanceof ReadDataReply){
                    ReadDataReply reply = (ReadDataReply) response;
                    //FIXME : A cast should not be required here ???
                    return (Optional<NormalizedNode<?, ?>>) Optional.of(reply.getNormalizedNode());
                }

                return Optional.absent();
            }
        };

        ListenableFutureTask<Optional<NormalizedNode<?, ?>>>
            future = ListenableFutureTask.create(call);

        //FIXME : Use a thread pool here
        Executors.newSingleThreadExecutor().submit(future);

        return future;
    }

    @Override
    public void write(InstanceIdentifier path, NormalizedNode<?, ?> data) {
        throw new UnsupportedOperationException("write");
    }

    @Override
    public void merge(InstanceIdentifier path, NormalizedNode<?, ?> data) {
        throw new UnsupportedOperationException("merge");
    }

    @Override
    public void delete(InstanceIdentifier path) {
        throw new UnsupportedOperationException("delete");
    }

    @Override
    public DOMStoreThreePhaseCommitCohort ready() {
        throw new UnsupportedOperationException("ready");
    }

    @Override
    public Object getIdentifier() {
        throw new UnsupportedOperationException("getIdentifier");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("close");
    }

    private ActorSelection remoteTransactionFromIdentifier(InstanceIdentifier path){
        String shardName = shardNameFromIdentifier(path);
        return remoteTransactionPaths.get(shardName);
    }

    private String shardNameFromIdentifier(InstanceIdentifier path){
        return Shard.DEFAULT_NAME;
    }
}
