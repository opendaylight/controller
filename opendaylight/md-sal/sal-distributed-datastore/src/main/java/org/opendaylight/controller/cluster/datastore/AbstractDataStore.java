/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import com.google.common.annotations.VisibleForTesting;
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
import org.opendaylight.controller.cluster.datastore.shardmanager.AbstractShardManagerCreator;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManagerCreator;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.datastore.utils.PrimaryShardInfoFutureCache;
import org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTreeChangePublisher;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of a distributed DOMStore.
 */
public abstract class AbstractDataStore implements DistributedDataStoreInterface, SchemaContextListener,
        DatastoreContextPropertiesUpdater.Listener, DOMStoreTreeChangePublisher,
        DOMDataTreeCommitCohortRegistry, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataStore.class);

    private final ActorUtils actorUtils;
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
        requireNonNull(actorSystem, "actorSystem should not be null");
        requireNonNull(cluster, "cluster should not be null");
        requireNonNull(configuration, "configuration should not be null");
        requireNonNull(datastoreContextFactory, "datastoreContextFactory should not be null");

        String shardManagerId = ShardManagerIdentifier.builder()
                .type(datastoreContextFactory.getBaseDatastoreContext().getDataStoreName()).build().toString();

        LOG.info("Creating ShardManager : {}", shardManagerId);

        String shardDispatcher =
                new Dispatchers(actorSystem.dispatchers()).getDispatcherPath(Dispatchers.DispatcherType.Shard);

        PrimaryShardInfoFutureCache primaryShardInfoCache = new PrimaryShardInfoFutureCache();

        AbstractShardManagerCreator<?> creator = getShardManagerCreator().cluster(cluster).configuration(configuration)
                .datastoreContextFactory(datastoreContextFactory)
                .waitTillReadyCountDownLatch(waitTillReadyCountDownLatch)
                .primaryShardInfoCache(primaryShardInfoCache)
                .restoreFromSnapshot(restoreFromSnapshot)
                .distributedDataStore(this);

        actorUtils = new ActorUtils(actorSystem, createShardManager(actorSystem, creator, shardDispatcher,
                shardManagerId), cluster, configuration, datastoreContextFactory.getBaseDatastoreContext(),
                primaryShardInfoCache);

        final Props clientProps = DistributedDataStoreClientActor.props(cluster.getCurrentMemberName(),
            datastoreContextFactory.getBaseDatastoreContext().getDataStoreName(), actorUtils);
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

        this.waitTillReadyTimeInMillis = computeWaitTillReadyTimeInMillis(actorUtils.getDatastoreContext());

        datastoreConfigMXBean = new DatastoreConfigurationMXBeanImpl(
                datastoreContextFactory.getBaseDatastoreContext().getDataStoreMXBeanType());
        datastoreConfigMXBean.setContext(datastoreContextFactory.getBaseDatastoreContext());
        datastoreConfigMXBean.registerMBean();

        datastoreInfoMXBean = new DatastoreInfoMXBeanImpl(datastoreContextFactory.getBaseDatastoreContext()
                .getDataStoreMXBeanType(), actorUtils);
        datastoreInfoMXBean.registerMBean();
    }

    @VisibleForTesting
    protected AbstractDataStore(final ActorUtils actorUtils, final ClientIdentifier identifier) {
        this.actorUtils = requireNonNull(actorUtils, "actorContext should not be null");
        this.client = null;
        this.identifier = requireNonNull(identifier);
        this.waitTillReadyTimeInMillis = computeWaitTillReadyTimeInMillis(actorUtils.getDatastoreContext());
    }

    @VisibleForTesting
    protected AbstractDataStore(final ActorUtils actorUtils, final ClientIdentifier identifier,
                                final DataStoreClient clientActor) {
        this.actorUtils = requireNonNull(actorUtils, "actorContext should not be null");
        this.client = clientActor;
        this.identifier = requireNonNull(identifier);
        this.waitTillReadyTimeInMillis = computeWaitTillReadyTimeInMillis(actorUtils.getDatastoreContext());
    }

    protected AbstractShardManagerCreator<?> getShardManagerCreator() {
        return new ShardManagerCreator();
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

    @Override
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(
            final YangInstanceIdentifier treeId, final L listener) {
        requireNonNull(treeId, "treeId should not be null");
        requireNonNull(listener, "listener should not be null");

        final String shardName = actorUtils.getShardStrategyFactory().getStrategy(treeId).findShard(treeId);
        LOG.debug("Registering tree listener: {} for tree: {} shard: {}", listener, treeId, shardName);

        final DataTreeChangeListenerProxy<L> listenerRegistrationProxy =
                new DataTreeChangeListenerProxy<>(actorUtils, listener, treeId);
        listenerRegistrationProxy.init(shardName);

        return listenerRegistrationProxy;
    }


    @Override
    public <C extends DOMDataTreeCommitCohort> DOMDataTreeCommitCohortRegistration<C> registerCommitCohort(
            final DOMDataTreeIdentifier subtree, final C cohort) {
        YangInstanceIdentifier treeId = requireNonNull(subtree, "subtree should not be null").getRootIdentifier();
        requireNonNull(cohort, "listener should not be null");


        final String shardName = actorUtils.getShardStrategyFactory().getStrategy(treeId).findShard(treeId);
        LOG.debug("Registering cohort: {} for tree: {} shard: {}", cohort, treeId, shardName);

        DataTreeCohortRegistrationProxy<C> cohortProxy =
                new DataTreeCohortRegistrationProxy<>(actorUtils, subtree, cohort);
        cohortProxy.init(shardName);
        return cohortProxy;
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext schemaContext) {
        actorUtils.setSchemaContext(schemaContext);
    }

    @Override
    public void onDatastoreContextUpdated(final DatastoreContextFactory contextFactory) {
        LOG.info("DatastoreContext updated for data store {}", actorUtils.getDataStoreName());

        actorUtils.setDatastoreContext(contextFactory);
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

        actorUtils.shutdown();

        if (client != null) {
            client.close();
        }
    }

    @Override
    public ActorUtils getActorUtils() {
        return actorUtils;
    }

    public void waitTillReady() {
        LOG.info("Beginning to wait for data store to become ready : {}", identifier);

        final boolean success;
        try {
            success = awaitReady();
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting for shards to settle", e);
            return;
        }

        if (success) {
            LOG.debug("Data store {} is now ready", identifier);
        } else {
            LOG.error("Shard leaders failed to settle in {} seconds, giving up",
                TimeUnit.MILLISECONDS.toSeconds(waitTillReadyTimeInMillis));
        }
    }

    private boolean awaitReady() throws InterruptedException {
        if (waitTillReadyTimeInMillis == 0) {
            waitTillReadyCountDownLatch.await();
            return true;
        }

        return waitTillReadyCountDownLatch.await(waitTillReadyTimeInMillis, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static ActorRef createShardManager(final ActorSystem actorSystem,
            final AbstractShardManagerCreator<?> creator, final String shardDispatcher,
            final String shardManagerId) {
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

        requireNonNull(shardLookup, "shardLookup should not be null");
        requireNonNull(insideShard, "insideShard should not be null");
        requireNonNull(delegate, "delegate should not be null");

        final String shardName = actorUtils.getShardStrategyFactory().getStrategy(shardLookup).findShard(shardLookup);
        LOG.debug("Registering tree listener: {} for tree: {} shard: {}, path inside shard: {}",
                delegate,shardLookup, shardName, insideShard);

        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> listenerRegistrationProxy =
                new DataTreeChangeListenerProxy<>(actorUtils,
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
        requireNonNull(delegate, "delegate should not be null");

        LOG.debug("Registering a listener for the configuration shard: {}", internalPath);

        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy =
                new DataTreeChangeListenerProxy<>(actorUtils, delegate, internalPath);
        proxy.init(ClusterUtils.PREFIX_CONFIG_SHARD_ID);

        return (ListenerRegistration<L>) proxy;
    }

    private static long computeWaitTillReadyTimeInMillis(final DatastoreContext context) {
        return context.getShardLeaderElectionTimeout().duration().toMillis()
                * context.getShardLeaderElectionTimeoutMultiplier();
    }
}
