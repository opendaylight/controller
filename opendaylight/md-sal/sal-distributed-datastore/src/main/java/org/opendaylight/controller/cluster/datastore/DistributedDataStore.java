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
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
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
import org.opendaylight.yangtools.util.PropertyUtils;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class DistributedDataStore implements DOMStore, SchemaContextListener, AutoCloseable {

    private static final Logger
        LOG = LoggerFactory.getLogger(DistributedDataStore.class);

    private static final String EXECUTOR_MAX_POOL_SIZE_PROP =
            "mdsal.dist-datastore-executor-pool.size";
    private static final int DEFAULT_EXECUTOR_MAX_POOL_SIZE = 10;

    private static final String EXECUTOR_MAX_QUEUE_SIZE_PROP =
            "mdsal.dist-datastore-executor-queue.size";
    private static final int DEFAULT_EXECUTOR_MAX_QUEUE_SIZE = 5000;

    private final String type;
    private final ActorContext actorContext;

    private SchemaContext schemaContext;

    /**
     * Executor used to run FutureTask's
     *
     * This is typically used when we need to make a request to an actor and
     * wait for it's response and the consumer needs to be provided a Future.
     */
    private final ListeningExecutorService executor =
            MoreExecutors.listeningDecorator(
                    SpecialExecutors.newBlockingBoundedFastThreadPool(
                            PropertyUtils.getIntSystemProperty(
                                    EXECUTOR_MAX_POOL_SIZE_PROP,
                                    DEFAULT_EXECUTOR_MAX_POOL_SIZE),
                            PropertyUtils.getIntSystemProperty(
                                    EXECUTOR_MAX_QUEUE_SIZE_PROP,
                                    DEFAULT_EXECUTOR_MAX_QUEUE_SIZE), "DistDataStore"));

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
    public <L extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L> registerChangeListener(
        YangInstanceIdentifier path, L listener,
        AsyncDataBroker.DataChangeScope scope) {

        ActorRef dataChangeListenerActor = actorContext.getActorSystem().actorOf(
            DataChangeListener.props(schemaContext,listener,path ));

        String shardName = ShardStrategyFactory.getStrategy(path).findShard(path);

        Object result = actorContext.executeLocalShardOperation(shardName,
            new RegisterChangeListener(path, dataChangeListenerActor.path(),
                scope),
            ActorContext.ASK_DURATION
        );

        if (result != null) {
            RegisterChangeListenerReply reply = (RegisterChangeListenerReply) result;
            return new DataChangeListenerRegistrationProxy(actorContext
                .actorSelection(reply.getListenerRegistrationPath()), listener,
                dataChangeListenerActor);
        }

        LOG.debug(
            "No local shard for shardName {} was found so returning a noop registration",
            shardName);
        return new NoOpDataChangeListenerRegistration(listener);
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
