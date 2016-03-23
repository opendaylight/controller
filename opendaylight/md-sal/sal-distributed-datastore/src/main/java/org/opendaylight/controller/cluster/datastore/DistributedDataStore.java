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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardManagerIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.DatastoreConfigurationMXBeanImpl;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.DatastoreInfoMXBeanImpl;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManager;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.Dispatchers;
import org.opendaylight.controller.cluster.datastore.utils.PrimaryShardInfoFutureCache;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTreeChangePublisher;
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
public class DistributedDataStore implements DOMStore, SchemaContextListener,
        DatastoreContextConfigAdminOverlay.Listener, DOMStoreTreeChangePublisher, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedDataStore.class);
    private static final String UNKNOWN_TYPE = "unknown";

    private static final long READY_WAIT_FACTOR = 3;

    private final ActorContext actorContext;
    private final long waitTillReadyTimeInMillis;

    private AutoCloseable closeable;

    private DatastoreConfigurationMXBeanImpl datastoreConfigMXBean;

    private DatastoreInfoMXBeanImpl datastoreInfoMXBean;

    private final CountDownLatch waitTillReadyCountDownLatch = new CountDownLatch(1);

    private final String type;

    private final TransactionContextFactory txContextFactory;

    public DistributedDataStore(ActorSystem actorSystem, ClusterWrapper cluster,
            Configuration configuration, DatastoreContextFactory datastoreContextFactory,
            DatastoreSnapshot restoreFromSnapshot) {
        Preconditions.checkNotNull(actorSystem, "actorSystem should not be null");
        Preconditions.checkNotNull(cluster, "cluster should not be null");
        Preconditions.checkNotNull(configuration, "configuration should not be null");
        Preconditions.checkNotNull(datastoreContextFactory, "datastoreContextFactory should not be null");

        this.type = datastoreContextFactory.getBaseDatastoreContext().getDataStoreName();

        String shardManagerId = ShardManagerIdentifier.builder().type(type).build().toString();

        LOG.info("Creating ShardManager : {}", shardManagerId);

        String shardDispatcher =
                new Dispatchers(actorSystem.dispatchers()).getDispatcherPath(Dispatchers.DispatcherType.Shard);

        PrimaryShardInfoFutureCache primaryShardInfoCache = new PrimaryShardInfoFutureCache();

        ShardManager.Builder builder = ShardManager.builder().cluster(cluster).configuration(configuration).
                datastoreContextFactory(datastoreContextFactory).waitTillReadyCountdownLatch(waitTillReadyCountDownLatch).
                primaryShardInfoCache(primaryShardInfoCache).restoreFromSnapshot(restoreFromSnapshot);

        actorContext = new ActorContext(actorSystem, createShardManager(actorSystem, builder, shardDispatcher,
                shardManagerId), cluster, configuration, datastoreContextFactory.getBaseDatastoreContext(), primaryShardInfoCache);

        this.waitTillReadyTimeInMillis =
                actorContext.getDatastoreContext().getShardLeaderElectionTimeout().duration().toMillis() * READY_WAIT_FACTOR;

        this.txContextFactory = TransactionContextFactory.create(actorContext);

        datastoreConfigMXBean = new DatastoreConfigurationMXBeanImpl(
                datastoreContextFactory.getBaseDatastoreContext().getDataStoreMXBeanType());
        datastoreConfigMXBean.setContext(datastoreContextFactory.getBaseDatastoreContext());
        datastoreConfigMXBean.registerMBean();

        datastoreInfoMXBean = new DatastoreInfoMXBeanImpl(datastoreContextFactory.getBaseDatastoreContext().
                getDataStoreMXBeanType(), actorContext);
        datastoreInfoMXBean.registerMBean();
    }

    @VisibleForTesting
    DistributedDataStore(ActorContext actorContext) {
        this.actorContext = Preconditions.checkNotNull(actorContext, "actorContext should not be null");
        this.txContextFactory = TransactionContextFactory.create(actorContext);
        this.type = UNKNOWN_TYPE;
        this.waitTillReadyTimeInMillis =
                actorContext.getDatastoreContext().getShardLeaderElectionTimeout().duration().toMillis() * READY_WAIT_FACTOR;
    }

    public void setCloseable(AutoCloseable closeable) {
        this.closeable = closeable;
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

        String shardName = actorContext.getShardStrategyFactory().getStrategy(path).findShard(path);

        final DataChangeListenerRegistrationProxy listenerRegistrationProxy =
                new DataChangeListenerRegistrationProxy(shardName, actorContext, listener);
        listenerRegistrationProxy.init(path, scope);

        return listenerRegistrationProxy;
    }

    @Override
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(YangInstanceIdentifier treeId, L listener) {
        Preconditions.checkNotNull(treeId, "treeId should not be null");
        Preconditions.checkNotNull(listener, "listener should not be null");

        final String shardName = actorContext.getShardStrategyFactory().getStrategy(treeId).findShard(treeId);
        LOG.debug("Registering tree listener: {} for tree: {} shard: {}", listener, treeId, shardName);

        final DataTreeChangeListenerProxy<L> listenerRegistrationProxy =
                new DataTreeChangeListenerProxy<L>(actorContext, listener);
        listenerRegistrationProxy.init(shardName, treeId);

        return listenerRegistrationProxy;
    }

    @Override
    public DOMStoreTransactionChain createTransactionChain() {
        return txContextFactory.createTransactionChain();
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
       return new TransactionProxy(txContextFactory, TransactionType.READ_ONLY);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        actorContext.acquireTxCreationPermit();
        return new TransactionProxy(txContextFactory, TransactionType.WRITE_ONLY);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        actorContext.acquireTxCreationPermit();
        return new TransactionProxy(txContextFactory, TransactionType.READ_WRITE);
    }

    @Override
    public void onGlobalContextUpdated(SchemaContext schemaContext) {
        actorContext.setSchemaContext(schemaContext);
    }

    @Override
    public void onDatastoreContextUpdated(DatastoreContextFactory contextFactory) {
        LOG.info("DatastoreContext updated for data store {}", actorContext.getDataStoreName());

        actorContext.setDatastoreContext(contextFactory);
        datastoreConfigMXBean.setContext(contextFactory.getBaseDatastoreContext());
    }

    @Override
    public void close() {
        LOG.info("Closing data store {}", type);

        if (datastoreConfigMXBean != null) {
            datastoreConfigMXBean.unregisterMBean();
        }
        if (datastoreInfoMXBean != null) {
            datastoreInfoMXBean.unregisterMBean();
        }

        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                LOG.debug("Error closing instance", e);
            }
        }

        txContextFactory.close();
        actorContext.shutdown();
    }

    public ActorContext getActorContext() {
        return actorContext;
    }

    public void waitTillReady(){
        LOG.info("Beginning to wait for data store to become ready : {}", type);

        try {
            if (waitTillReadyCountDownLatch.await(waitTillReadyTimeInMillis, TimeUnit.MILLISECONDS)) {
                LOG.debug("Data store {} is now ready", type);
            } else {
                LOG.error("Shared leaders failed to settle in {} seconds, giving up", TimeUnit.MILLISECONDS.toSeconds(waitTillReadyTimeInMillis));
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting for shards to settle", e);
        }
    }

    private static ActorRef createShardManager(ActorSystem actorSystem, ShardManager.Builder builder,
            String shardDispatcher, String shardManagerId) {
        Exception lastException = null;

        for(int i=0;i<100;i++) {
            try {
                return actorSystem.actorOf(builder.props().withDispatcher(shardDispatcher).withMailbox(
                        ActorContext.BOUNDED_MAILBOX), shardManagerId);
            } catch (Exception e){
                lastException = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                LOG.debug(String.format("Could not create actor %s because of %s - waiting for sometime before retrying (retry count = %d)", shardManagerId, e.getMessage(), i));
            }
        }

        throw new IllegalStateException("Failed to create Shard Manager", lastException);
    }

    @VisibleForTesting
    public CountDownLatch getWaitTillReadyCountDownLatch() {
        return waitTillReadyCountDownLatch;
    }
}
