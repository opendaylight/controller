/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Props;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.common.actor.Dispatchers.DispatcherType;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.databroker.actors.dds.DistributedDataStoreClientActor;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.shardmanager.AbstractShardManagerCreator;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManagerCreator;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManagerIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.datastore.utils.PrimaryShardInfoFutureCache;
import org.opendaylight.mdsal.dom.api.DOMDataBroker.CommitCohortExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTreeChangePublisher;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.jdk.javaapi.DurationConverters;

/**
 * Base implementation of a distributed DOMStore.
 */
public abstract class AbstractDataStore implements DistributedDataStoreInterface,
        DatastoreContextPropertiesUpdater.Listener, DOMStoreTreeChangePublisher, CommitCohortExtension,
        AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataStore.class);

    private final SettableFuture<Empty> readinessFuture = SettableFuture.create();
    private final ClientIdentifier identifier;
    private final DataStoreClient client;
    private final ActorUtils actorUtils;

    private AutoCloseable closeable;
    private DatastoreConfigurationMXBeanImpl datastoreConfigMXBean;
    private DatastoreInfoMXBeanImpl datastoreInfoMXBean;

    @SuppressWarnings("checkstyle:IllegalCatch")
    @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", justification = "Testing overrides")
    protected AbstractDataStore(final Path stateDir, final ActorSystem actorSystem, final ClusterWrapper cluster,
            final Configuration configuration, final DatastoreContextFactory datastoreContextFactory,
            final DatastoreSnapshot restoreFromSnapshot) {
        requireNonNull(actorSystem, "actorSystem should not be null");
        requireNonNull(cluster, "cluster should not be null");
        requireNonNull(configuration, "configuration should not be null");
        requireNonNull(datastoreContextFactory, "datastoreContextFactory should not be null");

        final var baseDatastoreContext = datastoreContextFactory.getBaseDatastoreContext();
        final var shardManagerId = new ShardManagerIdentifier(baseDatastoreContext.getDataStoreName());
        LOG.info("Creating ShardManager : {}", shardManagerId);

        final var shardDispatcher = DispatcherType.Shard.dispatcherPathIn(actorSystem);
        final var primaryShardInfoCache = new PrimaryShardInfoFutureCache();
        final var creator = getShardManagerCreator()
            .cluster(cluster)
            .configuration(configuration)
            .datastoreContextFactory(datastoreContextFactory)
            .readinessFuture(readinessFuture)
            .primaryShardInfoCache(primaryShardInfoCache)
            .restoreFromSnapshot(restoreFromSnapshot)
            .distributedDataStore(this);

        actorUtils = new ActorUtils(actorSystem,
            createShardManager(stateDir, actorSystem, creator, shardDispatcher, shardManagerId), cluster, configuration,
            baseDatastoreContext, primaryShardInfoCache);

        final Props clientProps = DistributedDataStoreClientActor.props(cluster.getCurrentMemberName(),
            datastoreContextFactory.getBaseDatastoreContext().getDataStoreName(), actorUtils);
        final ActorRef clientActor = actorSystem.actorOf(clientProps);
        try {
            client = DistributedDataStoreClientActor.getDistributedDataStoreClient(clientActor, 30, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.error("Failed to get actor for {}", clientProps, e);
            clientActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
            Throwables.throwIfUnchecked(e);
            throw new IllegalStateException(e);
        }

        identifier = client.getIdentifier();
        LOG.debug("Distributed data store client {} started", identifier);

        datastoreConfigMXBean = new DatastoreConfigurationMXBeanImpl(baseDatastoreContext.getDataStoreMXBeanType());
        datastoreConfigMXBean.setContext(baseDatastoreContext);
        datastoreConfigMXBean.registerMBean();

        datastoreInfoMXBean = new DatastoreInfoMXBeanImpl(baseDatastoreContext.getDataStoreMXBeanType(), actorUtils);
        datastoreInfoMXBean.registerMBean();
    }

    @VisibleForTesting
    protected AbstractDataStore(final ActorUtils actorUtils, final ClientIdentifier identifier,
                                final DataStoreClient clientActor) {
        this.actorUtils = requireNonNull(actorUtils, "actorContext should not be null");
        client = clientActor;
        this.identifier = requireNonNull(identifier);
    }

    @VisibleForTesting
    protected AbstractShardManagerCreator<?> getShardManagerCreator() {
        return new ShardManagerCreator();
    }

    protected final DataStoreClient getClient() {
        return client;
    }

    public void setCloseable(final AutoCloseable closeable) {
        this.closeable = closeable;
    }

    @Override
    public final Registration registerTreeChangeListener(final YangInstanceIdentifier treeId,
            final DOMDataTreeChangeListener listener) {
        return registerTreeChangeListener(treeId, listener, true);
    }

    private @NonNull Registration registerTreeChangeListener(final YangInstanceIdentifier treeId,
            final DOMDataTreeChangeListener listener, final boolean clustered) {
        requireNonNull(treeId, "treeId should not be null");
        requireNonNull(listener, "listener should not be null");

        /*
         * We need to potentially deal with multi-shard composition for registration targeting the root of the data
         * store. If that is the case, we delegate to a more complicated setup invol
         */
        if (treeId.isEmpty()) {
            // User is targeting root of the datastore. If there is more than one shard, we have to register with them
            // all and perform data composition.
            final var shardNames = actorUtils.getConfiguration().getAllShardNames();
            if (shardNames.size() > 1) {
                if (!clustered) {
                    throw new IllegalArgumentException(
                        "Cannot listen on root without non-clustered listener " + listener);
                }
                return new RootDataTreeChangeListenerProxy<>(actorUtils, listener, shardNames);
            }
        }

        final var shardName = actorUtils.getShardStrategyFactory().getStrategy(treeId).findShard(treeId);
        LOG.debug("Registering tree listener: {} for tree: {} shard: {}", listener, treeId, shardName);

        return DataTreeChangeListenerProxy.of(actorUtils, listener, treeId, clustered, shardName);
    }

    @Override
    @Deprecated(since = "9.0.0", forRemoval = true)
    public final Registration registerLegacyTreeChangeListener(final YangInstanceIdentifier treeId,
            final DOMDataTreeChangeListener listener) {
        return registerTreeChangeListener(treeId, listener, false);
    }

    @Override
    // Non-final for testing
    public Registration registerCommitCohort(final DOMDataTreeIdentifier subtree,
            final DOMDataTreeCommitCohort cohort) {
        YangInstanceIdentifier treeId = requireNonNull(subtree, "subtree should not be null").path();
        requireNonNull(cohort, "listener should not be null");


        final String shardName = actorUtils.getShardStrategyFactory().getStrategy(treeId).findShard(treeId);
        LOG.debug("Registering cohort: {} for tree: {} shard: {}", cohort, treeId, shardName);

        final var cohortProxy = new DataTreeCohortRegistrationProxy<>(actorUtils, subtree, cohort);
        cohortProxy.init(shardName);
        return cohortProxy;
    }

    public void onModelContextUpdated(final EffectiveModelContext newModelContext) {
        actorUtils.setSchemaContext(newModelContext);
    }

    @Override
    public final void onDatastoreContextUpdated(final DatastoreContextFactory contextFactory) {
        LOG.info("DatastoreContext updated for data store {}", actorUtils.getDataStoreName());

        actorUtils.setDatastoreContext(contextFactory);
        datastoreConfigMXBean.setContext(contextFactory.getBaseDatastoreContext());
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public final void close() {
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
    public final ActorUtils getActorUtils() {
        return actorUtils;
    }

    // TODO: consider removing this in favor of awaitReadiness()
    @Deprecated
    public final void waitTillReady() {
        LOG.info("Beginning to wait for data store to become ready : {}", identifier);

        final Duration toWait = initialSettleTime();
        try {
            if (!awaitReadiness(toWait)) {
                LOG.error("Shard leaders failed to settle in {}, giving up", toWait);
                return;
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting for shards to settle", e);
            return;
        }

        LOG.debug("Data store {} is now ready", identifier);
    }

    @Beta
    @Deprecated
    public final boolean awaitReadiness() throws InterruptedException {
        return awaitReadiness(initialSettleTime());
    }

    @Beta
    @Deprecated
    public final boolean awaitReadiness(final Duration toWait) throws InterruptedException {
        try {
            if (toWait.isFinite()) {
                try {
                    readinessFuture.get(toWait.toNanos(), TimeUnit.NANOSECONDS);
                } catch (TimeoutException e) {
                    LOG.debug("Timed out waiting for shards to settle", e);
                    return false;
                }
            } else {
                readinessFuture.get();
            }
        } catch (ExecutionException e) {
            LOG.warn("Unexpected readiness failure, assuming convergence", e);
        }

        return true;
    }

    @Beta
    @Deprecated
    public final void awaitReadiness(final long timeout, final TimeUnit unit)
            throws InterruptedException, TimeoutException {
        if (!awaitReadiness(Duration.create(timeout, unit))) {
            throw new TimeoutException("Shard leaders failed to settle");
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static ActorRef createShardManager(final Path stateDir, final ActorSystem actorSystem,
            final AbstractShardManagerCreator<?> creator, final String shardDispatcher,
            final ShardManagerIdentifier shardManagerId) {
        Exception lastException = null;

        for (int i = 0; i < 100; i++) {
            try {
                return actorSystem.actorOf(creator.props(stateDir).withDispatcher(shardDispatcher),
                    shardManagerId.toActorName());
            } catch (Exception e) {
                lastException = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                LOG.debug(
                    "Could not create actor {} because of {} - waiting for sometime before retrying (retry count = {})",
                    shardManagerId, e.getMessage(), i);
            }
        }

        throw new IllegalStateException("Failed to create Shard Manager", lastException);
    }

    /**
     * Future which completes when all shards settle for the first time.
     *
     * @return A Listenable future.
     */
    public final ListenableFuture<?> initialSettleFuture() {
        return readinessFuture;
    }

    @VisibleForTesting
    public final SettableFuture<Empty> readinessFuture() {
        return readinessFuture;
    }

    @Override
    public final Registration registerProxyListener(final YangInstanceIdentifier shardLookup,
            final YangInstanceIdentifier insideShard, final DOMDataTreeChangeListener delegate) {
        requireNonNull(shardLookup, "shardLookup should not be null");
        requireNonNull(insideShard, "insideShard should not be null");
        requireNonNull(delegate, "delegate should not be null");

        final var shardName = actorUtils.getShardStrategyFactory().getStrategy(shardLookup).findShard(shardLookup);
        LOG.debug("Registering tree listener: {} for tree: {} shard: {}, path inside shard: {}", delegate, shardLookup,
            shardName, insideShard);

        return DataTreeChangeListenerProxy.of(actorUtils, new DOMDataTreeChangeListener() {
            @Override
            public void onDataTreeChanged(final List<DataTreeCandidate> changes) {
                delegate.onDataTreeChanged(changes);
            }

            @Override
            public void onInitialData() {
                delegate.onInitialData();
            }
        }, insideShard, true, shardName);
    }

    private Duration initialSettleTime() {
        final DatastoreContext context = actorUtils.getDatastoreContext();
        final int multiplier = context.getInitialSettleTimeoutMultiplier();
        return multiplier == 0 ? Duration.Inf()
            : DurationConverters.toScala(context.getShardLeaderElectionTimeout().multipliedBy(multiplier));
    }
}
