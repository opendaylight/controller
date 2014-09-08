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
import akka.dispatch.OnComplete;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardManagerIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class DistributedDataStore implements DOMStore, SchemaContextListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedDataStore.class);

    private final ActorContext actorContext;

    public DistributedDataStore(ActorSystem actorSystem, String type, ClusterWrapper cluster,
            Configuration configuration, DatastoreContext datastoreContext) {
        Preconditions.checkNotNull(actorSystem, "actorSystem should not be null");
        Preconditions.checkNotNull(type, "type should not be null");
        Preconditions.checkNotNull(cluster, "cluster should not be null");
        Preconditions.checkNotNull(configuration, "configuration should not be null");
        Preconditions.checkNotNull(datastoreContext, "datastoreContext should not be null");

        String shardManagerId = ShardManagerIdentifier.builder().type(type).build().toString();

        LOG.info("Creating ShardManager : {}", shardManagerId);

        actorContext = new ActorContext(actorSystem, actorSystem.actorOf(
                ShardManager.props(type, cluster, configuration, datastoreContext)
                    .withMailbox(ActorContext.MAILBOX), shardManagerId ), cluster, configuration);

        actorContext.setOperationTimeout(datastoreContext.getOperationTimeoutInSeconds());
    }

    public DistributedDataStore(ActorContext actorContext) {
        this.actorContext = Preconditions.checkNotNull(actorContext, "actorContext should not be null");
    }

    @SuppressWarnings("unchecked")
    @Override
    public <L extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>
                                              ListenerRegistration<L> registerChangeListener(
        final YangInstanceIdentifier path, L listener,
        AsyncDataBroker.DataChangeScope scope) {

        Preconditions.checkNotNull(path, "path should not be null");
        Preconditions.checkNotNull(listener, "listener should not be null");

        LOG.debug("Registering listener: {} for path: {} scope: {}", listener, path, scope);

        ActorRef dataChangeListenerActor = actorContext.getActorSystem().actorOf(
            DataChangeListener.props(listener ));

        String shardName = ShardStrategyFactory.getStrategy(path).findShard(path);

        Future future = actorContext.executeLocalShardOperationAsync(shardName,
            new RegisterChangeListener(path, dataChangeListenerActor.path(), scope), new Timeout(2,
            TimeUnit.MINUTES));

        if (future != null) {
            final DataChangeListenerRegistrationProxy listenerRegistrationProxy =
                new DataChangeListenerRegistrationProxy(listener, dataChangeListenerActor);

            future.onComplete(new OnComplete(){

                @Override public void onComplete(Throwable failure, Object result)
                    throws Throwable {
                    if(failure != null){
                        LOG.error("Failed to register listener at path " + path.toString(), failure);
                        return;
                    }
                    RegisterChangeListenerReply reply = (RegisterChangeListenerReply) result;
                    listenerRegistrationProxy.setListenerRegistrationActor(actorContext
                        .actorSelection(reply.getListenerRegistrationPath()));
                }
            }, actorContext.getActorSystem().dispatcher());
            return listenerRegistrationProxy;
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
