/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import com.google.common.base.Preconditions;

import org.opendaylight.controller.cluster.datastore.identifiers.ShardManagerIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.concurrent.duration.Duration;

/**
 *
 */
public class DistributedDataStore implements DOMStore, SchemaContextListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedDataStore.class);

    private final ActorContext actorContext;
    private final ShardContext shardContext;

    public DistributedDataStore(ActorSystem actorSystem, String type, ClusterWrapper cluster,
            Configuration configuration, DistributedDataStoreProperties dataStoreProperties) {
        Preconditions.checkNotNull(actorSystem, "actorSystem should not be null");
        Preconditions.checkNotNull(type, "type should not be null");
        Preconditions.checkNotNull(cluster, "cluster should not be null");
        Preconditions.checkNotNull(configuration, "configuration should not be null");


        String shardManagerId = ShardManagerIdentifier.builder().type(type).build().toString();

        LOG.info("Creating ShardManager : {}", shardManagerId);

        shardContext = new ShardContext(InMemoryDOMDataStoreConfigProperties.create(
                dataStoreProperties.getMaxShardDataChangeExecutorPoolSize(),
                dataStoreProperties.getMaxShardDataChangeExecutorQueueSize(),
                dataStoreProperties.getMaxShardDataChangeListenerQueueSize()),
                Duration.create(dataStoreProperties.getShardTransactionIdleTimeoutInMinutes(),
                        TimeUnit.MINUTES));

        actorContext
                = new ActorContext(
                    actorSystem, actorSystem.actorOf(
                        ShardManager.props(type, cluster, configuration, shardContext).
                            withMailbox(ActorContext.MAILBOX), shardManagerId ), cluster, configuration);
    }

    public DistributedDataStore(ActorContext actorContext) {
        this.actorContext = Preconditions.checkNotNull(actorContext, "actorContext should not be null");
        this.shardContext = new ShardContext();
    }


    @SuppressWarnings("unchecked")
    @Override
    public <L extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>
                                              ListenerRegistration<L> registerChangeListener(
        YangInstanceIdentifier path, L listener,
        AsyncDataBroker.DataChangeScope scope) {

        Preconditions.checkNotNull(path, "path should not be null");
        Preconditions.checkNotNull(listener, "listener should not be null");

        LOG.debug("Registering listener: {} for path: {} scope: {}", listener, path, scope);

        ActorRef dataChangeListenerActor = actorContext.getActorSystem().actorOf(
            DataChangeListener.props(listener ));

        String shardName = ShardStrategyFactory.getStrategy(path).findShard(path);

        Object result = actorContext.executeLocalShardOperation(shardName,
            new RegisterChangeListener(path, dataChangeListenerActor.path(), scope),
            ActorContext.ASK_DURATION);

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
        return new TransactionChainProxy(actorContext);
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new TransactionProxy(actorContext, TransactionProxy.TransactionType.READ_ONLY);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new TransactionProxy(actorContext, TransactionProxy.TransactionType.WRITE_ONLY);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new TransactionProxy(actorContext, TransactionProxy.TransactionType.READ_WRITE);
    }

    @Override
    public void onGlobalContextUpdated(SchemaContext schemaContext) {
        actorContext.setSchemaContext(schemaContext);
    }

    @Override
    public void close() throws Exception {
        actorContext.shutdown();
    }
}
