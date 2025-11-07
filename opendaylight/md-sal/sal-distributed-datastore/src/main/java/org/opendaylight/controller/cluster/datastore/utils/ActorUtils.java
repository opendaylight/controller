/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static java.util.Objects.requireNonNull;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import java.lang.invoke.VarHandle;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;
import org.apache.pekko.actor.ActorPath;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.dispatch.ExecutionContexts;
import org.apache.pekko.dispatch.Mapper;
import org.apache.pekko.dispatch.OnComplete;
import org.apache.pekko.pattern.AskTimeoutException;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.common.actor.Dispatchers.DispatcherType;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContextFactory;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.UnknownMessageException;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalPrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.messages.RemotePrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.raft.client.messages.Shutdown;
import org.opendaylight.controller.cluster.reporting.MetricsReporter;
import org.opendaylight.yangtools.yang.data.tree.api.ReadOnlyDataTree;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;
import scala.jdk.javaapi.DurationConverters;

/**
 * The ActorUtils class contains utility methods which could be used by non-actors (like DistributedDataStore) to work
 * with actors a little more easily. An ActorContext can be freely passed around to local object instances but should
 * not be passed to actors especially remote actors.
 */
// Non-final for testing
public class ActorUtils {
    private static final class AskTimeoutCounter extends OnComplete<Object> implements BiConsumer<Object, Throwable> {
        private LongAdder ateExceptions = new LongAdder();

        @Override
        public void onComplete(final Throwable failure, final Object success) {
            accept(success, failure);
        }

        @Override
        public void accept(final Object success, final Throwable failure) {
            if (failure instanceof AskTimeoutException) {
                ateExceptions.increment();
            }
        }

        void reset() {
            ateExceptions = new LongAdder();
        }

        long sum() {
            return ateExceptions.sum();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ActorUtils.class);
    private static final String DISTRIBUTED_DATA_STORE_METRIC_REGISTRY = "distributed-data-store";
    private static final String METRIC_RATE = "rate";
    private static final Mapper<Throwable, Throwable> FIND_PRIMARY_FAILURE_TRANSFORMER = new Mapper<>() {
        @Override
        public Throwable apply(final Throwable failure) {
            if (failure instanceof AskTimeoutException) {
                // A timeout exception most likely means the shard isn't initialized.
                return new NotInitializedException(
                        "Timed out trying to find the primary shard. Most likely cause is the "
                        + "shard is not initialized yet.");
            }
            return failure;
        }
    };
    public static final String BOUNDED_MAILBOX = "bounded-mailbox";
    public static final String COMMIT = "commit";

    private final AskTimeoutCounter askTimeoutCounter = new AskTimeoutCounter();
    private final @NonNull ActorSystem actorSystem;
    private final ActorRef shardManager;
    private final ClusterWrapper clusterWrapper;
    private final Configuration configuration;
    private final String selfAddressHostPort;

    private DatastoreContext datastoreContext;
    private FiniteDuration operationDuration;
    private Timeout operationTimeout;
    private Timeout shardInitializationTimeout;

    private volatile EffectiveModelContext schemaContext;

    private final MetricRegistry metricRegistry = MetricsReporter.getInstance(DatastoreContext.METRICS_DOMAIN)
            .getMetricsRegistry();

    private final PrimaryShardInfoFutureCache primaryShardInfoCache;
    private final ShardStrategyFactory shardStrategyFactory;

    @VisibleForTesting
    public ActorUtils(final ActorSystem actorSystem, final ActorRef shardManager,
            final ClusterWrapper clusterWrapper, final Configuration configuration) {
        this(actorSystem, shardManager, clusterWrapper, configuration,
                DatastoreContext.newBuilder().build(), new PrimaryShardInfoFutureCache());
    }

    public ActorUtils(final ActorSystem actorSystem, final ActorRef shardManager,
            final ClusterWrapper clusterWrapper, final Configuration configuration,
            final DatastoreContext datastoreContext, final PrimaryShardInfoFutureCache primaryShardInfoCache) {
        this.actorSystem = requireNonNull(actorSystem);
        this.shardManager = shardManager;
        this.clusterWrapper = clusterWrapper;
        this.configuration = configuration;
        this.datastoreContext = datastoreContext;
        this.primaryShardInfoCache = primaryShardInfoCache;
        shardStrategyFactory = new ShardStrategyFactory(configuration);

        setCachedProperties();

        final var selfAddress = clusterWrapper.getSelfAddress();
        if (selfAddress != null && !selfAddress.host().isEmpty()) {
            selfAddressHostPort = selfAddress.host().get() + ":" + selfAddress.port().get();
        } else {
            selfAddressHostPort = null;
        }
    }

    private void setCachedProperties() {
        operationDuration = FiniteDuration.create(datastoreContext.getOperationTimeoutInMillis(),
            TimeUnit.MILLISECONDS);
        operationTimeout = new Timeout(operationDuration);
        shardInitializationTimeout = Timeout.create(datastoreContext.getShardInitializationTimeout().multipliedBy(2));
    }

    public DatastoreContext getDatastoreContext() {
        return datastoreContext;
    }

    public ActorSystem getActorSystem() {
        return actorSystem;
    }

    public ActorRef getShardManager() {
        return shardManager;
    }

    public ActorSelection actorSelection(final String actorPath) {
        return actorSystem.actorSelection(actorPath);
    }

    public ActorSelection actorSelection(final ActorPath actorPath) {
        return actorSystem.actorSelection(actorPath);
    }

    public void setSchemaContext(final EffectiveModelContext schemaContext) {
        this.schemaContext = schemaContext;

        if (shardManager != null) {
            shardManager.tell(new UpdateSchemaContext(schemaContext), ActorRef.noSender());
        }
    }

    public void setDatastoreContext(final DatastoreContextFactory contextFactory) {
        datastoreContext = contextFactory.getBaseDatastoreContext();
        setCachedProperties();

        // Trigger a write memory barrier so that the writes above will be published immediately even though they may
        // not be immediately visible to other threads due to unsynchronized reads. That is OK though - we are going for
        // eventual consistency here as immediately visible updates to these members are not critical. These members
        // could have been made volatile but wanted to avoid volatile reads as these are accessed often and updates will
        // be infrequent.
        VarHandle.fullFence();

        if (shardManager != null) {
            shardManager.tell(contextFactory, ActorRef.noSender());
        }
    }

    public EffectiveModelContext getSchemaContext() {
        return schemaContext;
    }

    /**
     * Issue a {@link Patterns#ask(ActorRef, Object, Duration)} and update {@link #getAskTimeoutExceptionCount()} if
     * the request results in {@link AskTimeoutException}.
     *
     * @param actor the actor
     * @param message the message
     * @param timeout the timeout.
     * @return the resulting CompletionStage
     */
    public CompletionStage<Object> ask(final ActorRef actor, final Object message, final Duration timeout) {
        final var ret = Patterns.ask(actor, message, timeout);
        ret.whenComplete(askTimeoutCounter);
        return ret;
    }

    public Future<PrimaryShardInfo> findPrimaryShardAsync(final String shardName) {
        final var ret = primaryShardInfoCache.getIfPresent(shardName);
        if (ret != null) {
            return ret;
        }

        return executeOperationAsync(shardManager, new FindPrimary(shardName, true), shardInitializationTimeout)
            .transform(new Mapper<>() {
                @Override
                public PrimaryShardInfo checkedApply(final Object response) throws UnknownMessageException {
                    return switch (response) {
                        case LocalPrimaryShardFound found -> {
                            LOG.debug("findPrimaryShardAsync received: {}", found);
                            yield onPrimaryShardFound(shardName, found.primaryPath(), DataStoreVersions.CURRENT_VERSION,
                                found.localShardDataTree());
                        }
                        case RemotePrimaryShardFound found -> {
                            LOG.debug("findPrimaryShardAsync received: {}", found);
                            yield onPrimaryShardFound(shardName, found.primaryPath(), found.primaryVersion(), null);
                        }
                        case NotInitializedException notInitialized -> throw notInitialized;
                        case PrimaryNotFoundException primaryNotFound -> throw primaryNotFound;
                        case NoShardLeaderException noShardLeader -> throw noShardLeader;
                        case null, default ->
                            throw new UnknownMessageException("FindPrimary returned unkown response: " + response);
                    };
                }
            }, FIND_PRIMARY_FAILURE_TRANSFORMER, getClientDispatcher());
    }

    private PrimaryShardInfo onPrimaryShardFound(final String shardName, final String primaryActorPath,
            final short primaryVersion, final ReadOnlyDataTree localShardDataTree) {
        final var actorSelection = actorSystem.actorSelection(primaryActorPath);
        final var info = localShardDataTree == null ? new PrimaryShardInfo(actorSelection, primaryVersion) :
            new PrimaryShardInfo(actorSelection, primaryVersion, localShardDataTree);
        primaryShardInfoCache.putSuccessful(shardName, info);
        return info;
    }

    /**
     * Finds a local shard given its shard name and return it's ActorRef.
     *
     * @param shardName the name of the local shard that needs to be found
     * @return a reference to a local shard actor which represents the shard
     *         specified by the shardName
     */
    @Deprecated(since = "11.0.2", forRemoval = true)
    public Optional<ActorRef> findLocalShard(final String shardName) {
        final var result = executeOperation(shardManager, new FindLocalShard(shardName, false));
        if (result instanceof LocalShardFound found) {
            LOG.debug("Local shard found {}", found.getPath());
            return Optional.of(found.getPath());
        }

        return Optional.empty();
    }

    /**
     * Finds a local shard async given its shard name and return a Future from which to obtain the
     * ActorRef.
     *
     * @param shardName the name of the local shard that needs to be found
     */
    public Future<ActorRef> findLocalShardAsync(final String shardName) {
        return executeOperationAsync(shardManager, new FindLocalShard(shardName, true), shardInitializationTimeout)
            .map(new Mapper<>() {
                @Override
                public ActorRef checkedApply(final Object response) throws UnknownMessageException {
                    return switch (response) {
                        case LocalShardFound found -> {
                            LOG.debug("Local shard found {}", found.getPath());
                            yield found.getPath();
                        }
                        case LocalShardNotFound notFound ->
                            throw new LocalShardNotFoundException("Local shard for " + shardName + " does not exist.");
                        case NotInitializedException notInitialized -> throw notInitialized;
                        case null, default ->
                            throw new UnknownMessageException("FindLocalShard returned unkown response: " + response);
                    };
                }
            }, getClientDispatcher());
    }

    /**
     * Executes an operation on a local actor and wait for it's response.
     *
     * @param actor the actor
     * @param message the message to send
     * @return The response of the operation
     */
    public Object executeOperation(final ActorRef actor, final Object message) {
        final var future = executeOperationAsync(actor, message, operationTimeout);

        try {
            return Await.result(future, operationDuration);
        } catch (InterruptedException | TimeoutException e) {
            throw new UncheckedTimeoutException("Sending message " + message.getClass().toString()
                    + " to actor " + actor.toString() + " failed. Try again later.", e);
        }
    }

    public Future<Object> executeOperationAsync(final ActorRef actor, final Object message, final Timeout timeout) {
        Preconditions.checkArgument(actor != null, "actor must not be null");
        Preconditions.checkArgument(message != null, "message must not be null");

        LOG.debug("Sending message {} to {}", message.getClass(), actor);
        return doAsk(actor, message, timeout);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void shutdown() {
        final var duration = DurationConverters.toScala(
            datastoreContext.getShardRaftConfig().getElectionTimeOutInterval().multipliedBy(3));
        try {
            Await.ready(Patterns.gracefulStop(shardManager, duration, Shutdown.INSTANCE), duration);
        } catch (Exception e) {
            LOG.warn("ShardManager for {} data store did not shutdown gracefully", getDataStoreName(), e);
        }
    }

    public ClusterWrapper getClusterWrapper() {
        return clusterWrapper;
    }

    public MemberName getCurrentMemberName() {
        return clusterWrapper.getCurrentMemberName();
    }

    public FiniteDuration getOperationDuration() {
        return operationDuration;
    }

    public Timeout getOperationTimeout() {
        return operationTimeout;
    }

    public boolean isPathLocal(final String path) {
        if (Strings.isNullOrEmpty(path)) {
            return false;
        }

        final int pathAtIndex = path.indexOf('@');
        if (pathAtIndex == -1) {
            //if the path is of local format, then its local and is co-located
            return true;
        }
        if (selfAddressHostPort == null) {
            // self address is local format and tx actor path is remote format
            return false;
        }

        // self-address and tx actor path, both are of remote path format
        final int slashIndex = path.indexOf('/', pathAtIndex);
        return slashIndex != -1 && selfAddressHostPort.equals(path.substring(pathAtIndex + 1, slashIndex));
    }

    /**
     * This is a utility method that lets us get a Timer object for any operation. This is a little open-ended to allow
     * us to create a timer for pretty much anything.
     *
     * @param operationName the name of the operation
     * @return the Timer instance
     */
    public Timer getOperationTimer(final String operationName) {
        return getOperationTimer(datastoreContext.getDataStoreName(), operationName);
    }

    public Timer getOperationTimer(final String dataStoreType, final String operationName) {
        final var rate = MetricRegistry.name(DISTRIBUTED_DATA_STORE_METRIC_REGISTRY, dataStoreType, operationName,
            METRIC_RATE);
        return metricRegistry.timer(rate);
    }

    /**
     * Get the name of the data store to which this ActorContext belongs.
     *
     * @return the data store name
     */
    public String getDataStoreName() {
        return datastoreContext.getDataStoreName();
    }

    public long getAskTimeoutExceptionCount() {
        return askTimeoutCounter.sum();
    }

    public void resetAskTimeoutExceptionCount() {
        askTimeoutCounter.reset();
    }

    /**
     * An akka dispatcher that is meant to be used when processing ask Futures which were triggered by client
     * code on the datastore.
     *
     * @return the dispatcher
     */
    public ExecutionContextExecutor getClientDispatcher() {
        return DispatcherType.Client.dispatcherIn(actorSystem);
    }

    public String getNotificationDispatcherPath() {
        return DispatcherType.Notification.dispatcherPathIn(actorSystem);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ShardStrategyFactory getShardStrategyFactory() {
        return shardStrategyFactory;
    }

    public PrimaryShardInfoFutureCache getPrimaryShardInfoCache() {
        return primaryShardInfoCache;
    }

    @VisibleForTesting
    Future<Object> doAsk(final ActorRef actorRef, final Object message, final Timeout timeout) {
        final var ret = Patterns.ask(actorRef, message, timeout);
        ret.onComplete(askTimeoutCounter, ExecutionContexts.parasitic());
        return ret;
    }
}
