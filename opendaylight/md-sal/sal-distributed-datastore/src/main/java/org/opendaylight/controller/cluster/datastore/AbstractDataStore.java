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
import akka.actor.PoisonPill;
import akka.actor.Props;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.common.actor.Dispatchers;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.databroker.actors.dds.DistributedDataStoreClientActor;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardManagerIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.DatastoreConfigurationMXBeanImpl;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.DatastoreInfoMXBeanImpl;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManagerCreator;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.datastore.utils.PrimaryShardInfoFutureCache;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTreeChangePublisher;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of a distributed DOMStore.
 */
public abstract class AbstractDataStore implements DistributedDataStoreInterface, SchemaContextListener,
        DatastoreContextConfigAdminOverlay.Listener, DOMStoreTreeChangePublisher,
        DOMDataTreeCommitCohortRegistry, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataStore.class);

    private static final long READY_WAIT_FACTOR = 3;

    private final ActorContext actorContext;
    private final long waitTillReadyTimeInMillis;

    private AutoCloseable closeable;

    private DatastoreConfigurationMXBeanImpl datastoreConfigMXBean;

    private DatastoreInfoMXBeanImpl datastoreInfoMXBean;

    private final CountDownLatch waitTillReadyCountDownLatch = new CountDownLatch(1);

    private final ClientIdentifier identifier;
    private final DataStoreClient client;

    @SuppressWarnings("checkstyle:IllegalCatch")
    protected AbstractDataStore(final ActorSystem actorSystem, final ClusterWrapper cluster,
            final Configuration configuration, final DatastoreContextFactory datastoreContextFactory,
            final DatastoreSnapshot restoreFromSnapshot) {
        Preconditions.checkNotNull(actorSystem, "actorSystem should not be null");
        Preconditions.checkNotNull(cluster, "cluster should not be null");
        Preconditions.checkNotNull(configuration, "configuration should not be null");
        Preconditions.checkNotNull(datastoreContextFactory, "datastoreContextFactory should not be null");

        String shardManagerId = ShardManagerIdentifier.builder()
                .type(datastoreContextFactory.getBaseDatastoreContext().getDataStoreName()).build().toString();

        LOG.info("Creating ShardManager : {}", shardManagerId);

        String shardDispatcher =
                new Dispatchers(actorSystem.dispatchers()).getDispatcherPath(Dispatchers.DispatcherType.Shard);

        PrimaryShardInfoFutureCache primaryShardInfoCache = new PrimaryShardInfoFutureCache();

        ShardManagerCreator creator = new ShardManagerCreator().cluster(cluster).configuration(configuration)
                .datastoreContextFactory(datastoreContextFactory)
                .waitTillReadyCountDownLatch(waitTillReadyCountDownLatch)
                .primaryShardInfoCache(primaryShardInfoCache)
                .restoreFromSnapshot(restoreFromSnapshot)
                .distributedDataStore(this);

        actorContext = new ActorContext(actorSystem, createShardManager(actorSystem, creator, shardDispatcher,
                shardManagerId), cluster, configuration, datastoreContextFactory.getBaseDatastoreContext(),
                primaryShardInfoCache);

        final Props clientProps = DistributedDataStoreClientActor.props(cluster.getCurrentMemberName(),
            datastoreContextFactory.getBaseDatastoreContext().getDataStoreName(), actorContext);
        final ActorRef clientActor = actorSystem.actorOf(clientProps);
        try {
            client = DistributedDataStoreClientActor.getDistributedDataStoreClient(clientActor, 30, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.error("Failed to get actor for {}", clientProps, e);
            clientActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }

        identifier = client.getIdentifier();
        LOG.debug("Distributed data store client {} started", identifier);

        this.waitTillReadyTimeInMillis = actorContext.getDatastoreContext().getShardLeaderElectionTimeout()
                .duration().toMillis() * READY_WAIT_FACTOR;

        datastoreConfigMXBean = new DatastoreConfigurationMXBeanImpl(
                datastoreContextFactory.getBaseDatastoreContext().getDataStoreMXBeanType());
        datastoreConfigMXBean.setContext(datastoreContextFactory.getBaseDatastoreContext());
        datastoreConfigMXBean.registerMBean();

        datastoreInfoMXBean = new DatastoreInfoMXBeanImpl(datastoreContextFactory.getBaseDatastoreContext()
                .getDataStoreMXBeanType(), actorContext);
        datastoreInfoMXBean.registerMBean();
    }

    @VisibleForTesting
    protected AbstractDataStore(final ActorContext actorContext, final ClientIdentifier identifier) {
        this.actorContext = Preconditions.checkNotNull(actorContext, "actorContext should not be null");
        this.client = null;
        this.identifier = Preconditions.checkNotNull(identifier);
        this.waitTillReadyTimeInMillis = actorContext.getDatastoreContext().getShardLeaderElectionTimeout()
                .duration().toMillis() * READY_WAIT_FACTOR;
    }

    @VisibleForTesting
    protected AbstractDataStore(final ActorContext actorContext, final ClientIdentifier identifier,
                                final DataStoreClient clientActor) {
        this.actorContext = Preconditions.checkNotNull(actorContext, "actorContext should not be null");
        this.client = clientActor;
        this.identifier = Preconditions.checkNotNull(identifier);
        this.waitTillReadyTimeInMillis = actorContext.getDatastoreContext().getShardLeaderElectionTimeout()
                .duration().toMillis() * READY_WAIT_FACTOR;
    }

    protected final DataStoreClient getClient() {
        return client;
    }

    final ClientIdentifier getIdentifier() {
        return identifier;
    }

    public void setCloseable(final AutoCloseable closeable) {
        this.closeable = closeable;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <L extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>
                                              ListenerRegistration<L> registerChangeListener(
        final YangInstanceIdentifier path, final L listener,
        final AsyncDataBroker.DataChangeScope scope) {

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
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(
            final YangInstanceIdentifier treeId, final L listener) {
        Preconditions.checkNotNull(treeId, "treeId should not be null");
        Preconditions.checkNotNull(listener, "listener should not be null");

        final String shardName = actorContext.getShardStrategyFactory().getStrategy(treeId).findShard(treeId);
        LOG.debug("Registering tree listener: {} for tree: {} shard: {}", listener, treeId, shardName);

        final DataTreeChangeListenerProxy<L> listenerRegistrationProxy =
                new DataTreeChangeListenerProxy<>(actorContext, listener, treeId);
        listenerRegistrationProxy.init(shardName);

        return listenerRegistrationProxy;
    }


    @Override
    public <C extends DOMDataTreeCommitCohort> DOMDataTreeCommitCohortRegistration<C> registerCommitCohort(
            final DOMDataTreeIdentifier subtree, final C cohort) {
        YangInstanceIdentifier treeId =
                Preconditions.checkNotNull(subtree, "subtree should not be null").getRootIdentifier();
        Preconditions.checkNotNull(cohort, "listener should not be null");


        final String shardName = actorContext.getShardStrategyFactory().getStrategy(treeId).findShard(treeId);
        LOG.debug("Registering cohort: {} for tree: {} shard: {}", cohort, treeId, shardName);

        DataTreeCohortRegistrationProxy<C> cohortProxy =
                new DataTreeCohortRegistrationProxy<>(actorContext, subtree, cohort);
        cohortProxy.init(shardName);
        return cohortProxy;
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext schemaContext) {
        actorContext.setSchemaContext(schemaContext);
    }

    @Override
    public void onDatastoreContextUpdated(final DatastoreContextFactory contextFactory) {
        LOG.info("DatastoreContext updated for data store {}", actorContext.getDataStoreName());

        actorContext.setDatastoreContext(contextFactory);
        datastoreConfigMXBean.setContext(contextFactory.getBaseDatastoreContext());
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void close() {
        LOG.info("Closing data store {}", identifier);

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

        actorContext.shutdown();

        if (client != null) {
            client.close();
        }
    }

    @Override
    public ActorContext getActorContext() {
        return actorContext;
    }

    public void waitTillReady() {
        LOG.info("Beginning to wait for data store to become ready : {}", identifier);

        try {
            if (waitTillReadyCountDownLatch.await(waitTillReadyTimeInMillis, TimeUnit.MILLISECONDS)) {
                LOG.debug("Data store {} is now ready", identifier);
            } else {
                LOG.error("Shard leaders failed to settle in {} seconds, giving up",
                        TimeUnit.MILLISECONDS.toSeconds(waitTillReadyTimeInMillis));
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting for shards to settle", e);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static ActorRef createShardManager(final ActorSystem actorSystem, final ShardManagerCreator creator,
            final String shardDispatcher, final String shardManagerId) {
        Exception lastException = null;

        for (int i = 0; i < 100; i++) {
            try {
                return actorSystem.actorOf(creator.props().withDispatcher(shardDispatcher), shardManagerId);
            } catch (Exception e) {
                lastException = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                LOG.debug("Could not create actor {} because of {} - waiting for sometime before retrying "
                        + "(retry count = {})", shardManagerId, e.getMessage(), i);
            }
        }

        throw new IllegalStateException("Failed to create Shard Manager", lastException);
    }

    @VisibleForTesting
    public CountDownLatch getWaitTillReadyCountDownLatch() {
        return waitTillReadyCountDownLatch;
    }

    @SuppressWarnings("unchecked")
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerProxyListener(
            final YangInstanceIdentifier shardLookup,
            final YangInstanceIdentifier insideShard,
            final org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener delegate) {

        Preconditions.checkNotNull(shardLookup, "shardLookup should not be null");
        Preconditions.checkNotNull(insideShard, "insideShard should not be null");
        Preconditions.checkNotNull(delegate, "delegate should not be null");

        final String shardName = actorContext.getShardStrategyFactory().getStrategy(shardLookup).findShard(shardLookup);
        LOG.debug("Registering tree listener: {} for tree: {} shard: {}, path inside shard: {}",
                delegate,shardLookup, shardName, insideShard);

        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> listenerRegistrationProxy =
                new DataTreeChangeListenerProxy<>(actorContext,
                        // wrap this in the ClusteredDOMDataTreeChangeLister interface
                        // since we always want clustered registration
                        (ClusteredDOMDataTreeChangeListener) delegate::onDataTreeChanged, insideShard);
        listenerRegistrationProxy.init(shardName);

        return (ListenerRegistration<L>) listenerRegistrationProxy;
    }

    @SuppressWarnings("unchecked")
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerShardConfigListener(
            final YangInstanceIdentifier internalPath,
            final DOMDataTreeChangeListener delegate) {
        Preconditions.checkNotNull(delegate, "delegate should not be null");

        LOG.debug("Registering a listener for the configuration shard: {}", internalPath);

        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy =
                new DataTreeChangeListenerProxy<>(actorContext, delegate, internalPath);
        proxy.init(ClusterUtils.PREFIX_CONFIG_SHARD_ID);

        return (ListenerRegistration<L>) proxy;
    }

}
