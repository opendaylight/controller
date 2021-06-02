/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static akka.pattern.Patterns.ask;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.pattern.AskTimeoutException;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.common.actor.Dispatchers;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContextFactory;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException;
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
import org.opendaylight.yangtools.yang.data.api.schema.tree.ReadOnlyDataTree;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * The ActorUtils class contains utility methods which could be used by non-actors (like DistributedDataStore) to work
 * with actors a little more easily. An ActorContext can be freely passed around to local object instances but should
 * not be passed to actors especially remote actors.
 */
public class ActorUtils {
    private static final class AskTimeoutCounter extends OnComplete<Object> implements ExecutionContext {
        private LongAdder ateExceptions = new LongAdder();

        @Override
        public void onComplete(final Throwable failure, final Object success) throws Throwable {
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

        @Override
        public void execute(final Runnable runnable) {
            // Yes, we are this ugly, but then we are just doing a check + an increment
            runnable.run();
        }

        @Override
        public void reportFailure(final Throwable cause) {
            LOG.warn("Unexpected failure updating counters", cause);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ActorUtils.class);
    private static final String DISTRIBUTED_DATA_STORE_METRIC_REGISTRY = "distributed-data-store";
    private static final String METRIC_RATE = "rate";
    private static final Mapper<Throwable, Throwable> FIND_PRIMARY_FAILURE_TRANSFORMER = new Mapper<>() {
        @Override
        public Throwable apply(final Throwable failure) {
            Throwable actualFailure = failure;
            if (failure instanceof AskTimeoutException) {
                // A timeout exception most likely means the shard isn't initialized.
                actualFailure = new NotInitializedException(
                        "Timed out trying to find the primary shard. Most likely cause is the "
                        + "shard is not initialized yet.");
            }

            return actualFailure;
        }
    };
    public static final String BOUNDED_MAILBOX = "bounded-mailbox";
    public static final String COMMIT = "commit";

    private final AskTimeoutCounter askTimeoutCounter = new AskTimeoutCounter();
    private final ActorSystem actorSystem;
    private final ActorRef shardManager;
    private final ClusterWrapper clusterWrapper;
    private final Configuration configuration;
    private final String selfAddressHostPort;
    private final Dispatchers dispatchers;

    private DatastoreContext datastoreContext;
    private FiniteDuration operationDuration;
    private Timeout operationTimeout;
    private TransactionRateLimiter txRateLimiter;
    private Timeout transactionCommitOperationTimeout;
    private Timeout shardInitializationTimeout;

    private volatile EffectiveModelContext schemaContext;

    // Used as a write memory barrier.
    @SuppressWarnings("unused")
    private volatile boolean updated;

    private final MetricRegistry metricRegistry = MetricsReporter.getInstance(DatastoreContext.METRICS_DOMAIN)
            .getMetricsRegistry();

    private final PrimaryShardInfoFutureCache primaryShardInfoCache;
    private final ShardStrategyFactory shardStrategyFactory;

    public ActorUtils(final ActorSystem actorSystem, final ActorRef shardManager,
            final ClusterWrapper clusterWrapper, final Configuration configuration) {
        this(actorSystem, shardManager, clusterWrapper, configuration,
                DatastoreContext.newBuilder().build(), new PrimaryShardInfoFutureCache());
    }

    public ActorUtils(final ActorSystem actorSystem, final ActorRef shardManager,
            final ClusterWrapper clusterWrapper, final Configuration configuration,
            final DatastoreContext datastoreContext, final PrimaryShardInfoFutureCache primaryShardInfoCache) {
        this.actorSystem = actorSystem;
        this.shardManager = shardManager;
        this.clusterWrapper = clusterWrapper;
        this.configuration = configuration;
        this.datastoreContext = datastoreContext;
        this.dispatchers = new Dispatchers(actorSystem.dispatchers());
        this.primaryShardInfoCache = primaryShardInfoCache;
        this.shardStrategyFactory = new ShardStrategyFactory(configuration);

        setCachedProperties();

        Address selfAddress = clusterWrapper.getSelfAddress();
        if (selfAddress != null && !selfAddress.host().isEmpty()) {
            selfAddressHostPort = selfAddress.host().get() + ":" + selfAddress.port().get();
        } else {
            selfAddressHostPort = null;
        }
    }

    private void setCachedProperties() {
        txRateLimiter = new TransactionRateLimiter(this);

        operationDuration = FiniteDuration.create(datastoreContext.getOperationTimeoutInMillis(),
            TimeUnit.MILLISECONDS);
        operationTimeout = new Timeout(operationDuration);

        transactionCommitOperationTimeout =  new Timeout(FiniteDuration.create(
                datastoreContext.getShardTransactionCommitTimeoutInSeconds(), TimeUnit.SECONDS));

        shardInitializationTimeout = new Timeout(datastoreContext.getShardInitializationTimeout().duration().$times(2));
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
        this.datastoreContext = contextFactory.getBaseDatastoreContext();
        setCachedProperties();

        // We write the 'updated' volatile to trigger a write memory barrier so that the writes above
        // will be published immediately even though they may not be immediately visible to other
        // threads due to unsynchronized reads. That's OK though - we're going for eventual
        // consistency here as immediately visible updates to these members aren't critical. These
        // members could've been made volatile but wanted to avoid volatile reads as these are
        // accessed often and updates will be infrequent.

        updated = true;

        if (shardManager != null) {
            shardManager.tell(contextFactory, ActorRef.noSender());
        }
    }

    public EffectiveModelContext getSchemaContext() {
        return schemaContext;
    }

    public Future<PrimaryShardInfo> findPrimaryShardAsync(final String shardName) {
        Future<PrimaryShardInfo> ret = primaryShardInfoCache.getIfPresent(shardName);
        if (ret != null) {
            return ret;
        }
        Future<Object> future = executeOperationAsync(shardManager,
                new FindPrimary(shardName, true), shardInitializationTimeout);

        return future.transform(new Mapper<Object, PrimaryShardInfo>() {
            @Override
            public PrimaryShardInfo checkedApply(final Object response) throws UnknownMessageException {
                if (response instanceof RemotePrimaryShardFound) {
                    LOG.debug("findPrimaryShardAsync received: {}", response);
                    RemotePrimaryShardFound found = (RemotePrimaryShardFound)response;
                    return onPrimaryShardFound(shardName, found.getPrimaryPath(), found.getPrimaryVersion(), null);
                } else if (response instanceof LocalPrimaryShardFound) {
                    LOG.debug("findPrimaryShardAsync received: {}", response);
                    LocalPrimaryShardFound found = (LocalPrimaryShardFound)response;
                    return onPrimaryShardFound(shardName, found.getPrimaryPath(), DataStoreVersions.CURRENT_VERSION,
                            found.getLocalShardDataTree());
                } else if (response instanceof NotInitializedException) {
                    throw (NotInitializedException)response;
                } else if (response instanceof PrimaryNotFoundException) {
                    throw (PrimaryNotFoundException)response;
                } else if (response instanceof NoShardLeaderException) {
                    throw (NoShardLeaderException)response;
                }

                throw new UnknownMessageException(String.format(
                        "FindPrimary returned unkown response: %s", response));
            }
        }, FIND_PRIMARY_FAILURE_TRANSFORMER, getClientDispatcher());
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private PrimaryShardInfo onPrimaryShardFound(final String shardName, final String primaryActorPath,
            final short primaryVersion, final ReadOnlyDataTree localShardDataTree) {
        ActorSelection actorSelection = actorSystem.actorSelection(primaryActorPath);
        PrimaryShardInfo info = localShardDataTree == null ? new PrimaryShardInfo(actorSelection, primaryVersion) :
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
    public Optional<ActorRef> findLocalShard(final String shardName) {
        Object result = executeOperation(shardManager, new FindLocalShard(shardName, false));

        if (result instanceof LocalShardFound) {
            LocalShardFound found = (LocalShardFound) result;
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
        Future<Object> future = executeOperationAsync(shardManager,
                new FindLocalShard(shardName, true), shardInitializationTimeout);

        return future.map(new Mapper<Object, ActorRef>() {
            @Override
            public ActorRef checkedApply(final Object response) throws Throwable {
                if (response instanceof LocalShardFound) {
                    LocalShardFound found = (LocalShardFound)response;
                    LOG.debug("Local shard found {}", found.getPath());
                    return found.getPath();
                } else if (response instanceof NotInitializedException) {
                    throw (NotInitializedException)response;
                } else if (response instanceof LocalShardNotFound) {
                    throw new LocalShardNotFoundException(
                            String.format("Local shard for %s does not exist.", shardName));
                }

                throw new UnknownMessageException(String.format(
                        "FindLocalShard returned unkown response: %s", response));
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
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Object executeOperation(final ActorRef actor, final Object message) {
        Future<Object> future = executeOperationAsync(actor, message, operationTimeout);

        try {
            return Await.result(future, operationDuration);
        } catch (Exception e) {
            throw new TimeoutException("Sending message " + message.getClass().toString()
                    + " to actor " + actor.toString() + " failed. Try again later.", e);
        }
    }

    /**
     * Execute an operation on a remote actor and wait for it's response.
     *
     * @param actor the actor
     * @param message the message
     * @return the response message
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Object executeOperation(final ActorSelection actor, final Object message) {
        Future<Object> future = executeOperationAsync(actor, message);

        try {
            return Await.result(future, operationDuration);
        } catch (Exception e) {
            throw new TimeoutException("Sending message " + message.getClass().toString()
                    + " to actor " + actor.toString() + " failed. Try again later.", e);
        }
    }

    public Future<Object> executeOperationAsync(final ActorRef actor, final Object message, final Timeout timeout) {
        Preconditions.checkArgument(actor != null, "actor must not be null");
        Preconditions.checkArgument(message != null, "message must not be null");

        LOG.debug("Sending message {} to {}", message.getClass(), actor);
        return doAsk(actor, message, timeout);
    }

    /**
     * Execute an operation on a remote actor asynchronously.
     *
     * @param actor the ActorSelection
     * @param message the message to send
     * @param timeout the operation timeout
     * @return a Future containing the eventual result
     */
    public Future<Object> executeOperationAsync(final ActorSelection actor, final Object message,
            final Timeout timeout) {
        Preconditions.checkArgument(actor != null, "actor must not be null");
        Preconditions.checkArgument(message != null, "message must not be null");

        LOG.debug("Sending message {} to {}", message.getClass(), actor);

        return doAsk(actor, message, timeout);
    }

    /**
     * Execute an operation on a remote actor asynchronously.
     *
     * @param actor the ActorSelection
     * @param message the message to send
     * @return a Future containing the eventual result
     */
    public Future<Object> executeOperationAsync(final ActorSelection actor, final Object message) {
        return executeOperationAsync(actor, message, operationTimeout);
    }

    /**
     * Sends an operation to be executed by a remote actor asynchronously without waiting for a
     * reply (essentially set and forget).
     *
     * @param actor the ActorSelection
     * @param message the message to send
     */
    public void sendOperationAsync(final ActorSelection actor, final Object message) {
        Preconditions.checkArgument(actor != null, "actor must not be null");
        Preconditions.checkArgument(message != null, "message must not be null");

        LOG.debug("Sending message {} to {}", message.getClass(), actor);

        actor.tell(message, ActorRef.noSender());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void shutdown() {
        FiniteDuration duration = datastoreContext.getShardRaftConfig().getElectionTimeOutInterval().$times(3);
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

    /**
     * Send the message to each and every shard.
     */
    public void broadcast(final Function<Short, Object> messageSupplier, final Class<?> messageClass) {
        for (final String shardName : configuration.getAllShardNames()) {

            Future<PrimaryShardInfo> primaryFuture = findPrimaryShardAsync(shardName);
            primaryFuture.onComplete(new OnComplete<PrimaryShardInfo>() {
                @Override
                public void onComplete(final Throwable failure, final PrimaryShardInfo primaryShardInfo) {
                    if (failure != null) {
                        LOG.warn("broadcast failed to send message {} to shard {}", messageClass.getSimpleName(),
                            shardName, failure);
                    } else {
                        Object message = messageSupplier.apply(primaryShardInfo.getPrimaryShardVersion());
                        primaryShardInfo.getPrimaryShardActor().tell(message, ActorRef.noSender());
                    }
                }
            }, getClientDispatcher());
        }
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

        int pathAtIndex = path.indexOf('@');
        if (pathAtIndex == -1) {
            //if the path is of local format, then its local and is co-located
            return true;

        } else if (selfAddressHostPort != null) {
            // self-address and tx actor path, both are of remote path format
            int slashIndex = path.indexOf('/', pathAtIndex);

            if (slashIndex == -1) {
                return false;
            }

            String hostPort = path.substring(pathAtIndex + 1, slashIndex);
            return hostPort.equals(selfAddressHostPort);

        } else {
            // self address is local format and tx actor path is remote format
            return false;
        }
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
        final String rate = MetricRegistry.name(DISTRIBUTED_DATA_STORE_METRIC_REGISTRY, dataStoreType,
                operationName, METRIC_RATE);
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

    /**
     * Get the current transaction creation rate limit.
     *
     * @return the rate limit
     */
    public double getTxCreationLimit() {
        return txRateLimiter.getTxCreationLimit();
    }

    public long getAskTimeoutExceptionCount() {
        return askTimeoutCounter.sum();
    }

    public void resetAskTimeoutExceptionCount() {
        askTimeoutCounter.reset();
    }

    /**
     * Try to acquire a transaction creation permit. Will block if no permits are available.
     */
    public void acquireTxCreationPermit() {
        txRateLimiter.acquire();
    }

    /**
     * Returns the operation timeout to be used when committing transactions.
     *
     * @return the operation timeout
     */
    public Timeout getTransactionCommitOperationTimeout() {
        return transactionCommitOperationTimeout;
    }

    /**
     * An akka dispatcher that is meant to be used when processing ask Futures which were triggered by client
     * code on the datastore.
     *
     * @return the dispatcher
     */
    public ExecutionContext getClientDispatcher() {
        return this.dispatchers.getDispatcher(Dispatchers.DispatcherType.Client);
    }

    public String getNotificationDispatcherPath() {
        return this.dispatchers.getDispatcherPath(Dispatchers.DispatcherType.Notification);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ShardStrategyFactory getShardStrategyFactory() {
        return shardStrategyFactory;
    }

    protected Future<Object> doAsk(final ActorRef actorRef, final Object message, final Timeout timeout) {
        return ask(actorRef, message, timeout);
    }

    protected Future<Object> doAsk(final ActorSelection actorRef, final Object message, final Timeout timeout) {
        final Future<Object> ret = ask(actorRef, message, timeout);
        ret.onComplete(askTimeoutCounter, askTimeoutCounter);
        return ret;
    }

    public PrimaryShardInfoFutureCache getPrimaryShardInfoCache() {
        return primaryShardInfoCache;
    }
}
