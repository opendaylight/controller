/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardManagerIdentifier;
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

/**
 *
 */
public class DistributedDataStore implements DOMStore, SchemaContextListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedDataStore.class);
    public static final int REGISTER_DATA_CHANGE_LISTENER_TIMEOUT_FACTOR = 24; // 24 times the usual operation timeout

    private final ActorContext actorContext;

    public DistributedDataStore(ActorSystem actorSystem, ClusterWrapper cluster,
            Configuration configuration, DatastoreContext datastoreContext) {
        Preconditions.checkNotNull(actorSystem, "actorSystem should not be null");
        Preconditions.checkNotNull(cluster, "cluster should not be null");
        Preconditions.checkNotNull(configuration, "configuration should not be null");
        Preconditions.checkNotNull(datastoreContext, "datastoreContext should not be null");

        String type = datastoreContext.getDataStoreType();

        String shardManagerId = ShardManagerIdentifier.builder().type(type).build().toString();

        LOG.info("Creating ShardManager : {}", shardManagerId);

        actorContext = new ActorContext(actorSystem, actorSystem.actorOf(
                ShardManager.props(cluster, configuration, datastoreContext)
                    .withMailbox(ActorContext.MAILBOX), shardManagerId ),
                cluster, configuration, datastoreContext);
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

        String shardName = ShardStrategyFactory.getStrategy(path).findShard(path);

        final DataChangeListenerRegistrationProxy listenerRegistrationProxy =
                new DataChangeListenerRegistrationProxy(shardName, actorContext, listener);
        listenerRegistrationProxy.init(path, scope);

        return listenerRegistrationProxy;
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
        actorContext.acquireTxCreationPermit();
        return new TransactionProxy(actorContext, TransactionProxy.TransactionType.WRITE_ONLY);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        actorContext.acquireTxCreationPermit();
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

    @VisibleForTesting
    ActorContext getActorContext() {
        return actorContext;
    }
}
