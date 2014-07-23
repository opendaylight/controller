/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class DistributedDataStore implements DOMStore, SchemaContextListener, AutoCloseable {

    private static final Logger
        LOG = LoggerFactory.getLogger(DistributedDataStore.class);


    private final String type;
    private final ActorContext actorContext;

    private SchemaContext schemaContext;



    /**
     * Executor used to run FutureTask's
     *
     * This is typically used when we need to make a request to an actor and
     * wait for it's response and the consumer needs to be provided a Future.
     *
     * FIXME : Make the thread pool configurable
     */
    private final ExecutorService executor =
        Executors.newFixedThreadPool(10);

    public DistributedDataStore(ActorSystem actorSystem, String type, ClusterWrapper cluster, Configuration configuration) {
        this(new ActorContext(actorSystem, actorSystem
            .actorOf(ShardManager.props(type, cluster, configuration),
                "shardmanager-" + type), cluster, configuration), type);
    }

    public DistributedDataStore(ActorContext actorContext, String type) {
        this.type = type;
        this.actorContext = actorContext;
    }


    @Override
    public <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L> registerChangeListener(
        InstanceIdentifier path, L listener,
        AsyncDataBroker.DataChangeScope scope) {

        ActorRef dataChangeListenerActor = actorContext.getActorSystem().actorOf(
            DataChangeListener.props(schemaContext,listener,path ));

        String shardName = ShardStrategyFactory.getStrategy(path).findShard(path);

        Object result = actorContext.executeShardOperation(shardName,
            new RegisterChangeListener(path, dataChangeListenerActor.path(),
                scope).toSerializable(),
            ActorContext.ASK_DURATION
        );

        RegisterChangeListenerReply reply = RegisterChangeListenerReply.fromSerializable(actorContext.getActorSystem(),result);
        return new DataChangeListenerRegistrationProxy(actorContext.actorSelection(reply.getListenerRegistrationPath()), listener, dataChangeListenerActor);
    }



    @Override
    public DOMStoreTransactionChain createTransactionChain() {
        return new TransactionChainProxy(actorContext, executor, schemaContext);
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new TransactionProxy(actorContext, TransactionProxy.TransactionType.READ_ONLY,
            executor, schemaContext);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new TransactionProxy(actorContext, TransactionProxy.TransactionType.WRITE_ONLY,
            executor, schemaContext);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new TransactionProxy(actorContext, TransactionProxy.TransactionType.READ_WRITE,
            executor, schemaContext);
    }

    @Override public void onGlobalContextUpdated(SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
        actorContext.getShardManager().tell(
            new UpdateSchemaContext(schemaContext), null);
    }

    @Override public void close() throws Exception {
        actorContext.shutdown();

    }
}
