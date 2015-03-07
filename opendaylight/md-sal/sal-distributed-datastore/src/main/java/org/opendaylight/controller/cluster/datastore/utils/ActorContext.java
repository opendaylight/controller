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
import akka.actor.PoisonPill;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.pattern.AskTimeoutException;
import akka.util.Timeout;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.RateLimiter;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.common.actor.CommonConfig;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.Configuration;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException;
import org.opendaylight.controller.cluster.datastore.exceptions.UnknownMessageException;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * The ActorContext class contains utility methods which could be used by
 * non-actors (like DistributedDataStore) to work with actors a little more
 * easily. An ActorContext can be freely passed around to local object instances
 * but should not be passed to actors especially remote actors
 */
public class ActorContext {
    private static final Logger LOG = LoggerFactory.getLogger(ActorContext.class);
    private static final String UNKNOWN_DATA_STORE_TYPE = "unknown";
    private static final String DISTRIBUTED_DATA_STORE_METRIC_REGISTRY = "distributed-data-store";
    private static final String METRIC_RATE = "rate";
    private static final String DOMAIN = "org.opendaylight.controller.cluster.datastore";
    private static final Mapper<Throwable, Throwable> FIND_PRIMARY_FAILURE_TRANSFORMER =
                                                              new Mapper<Throwable, Throwable>() {
        @Override
        public Throwable apply(Throwable failure) {
            Throwable actualFailure = failure;
            if(failure instanceof AskTimeoutException) {
                // A timeout exception most likely means the shard isn't initialized.
                actualFailure = new NotInitializedException(
                        "Timed out trying to find the primary shard. Most likely cause is the " +
                        "shard is not initialized yet.");
            }

            return actualFailure;
        }
    };
    public static final String MAILBOX = "bounded-mailbox";

    private final ActorSystem actorSystem;
    private final ActorRef shardManager;
    private final ClusterWrapper clusterWrapper;
    private final Configuration configuration;
    private DatastoreContext datastoreContext;
    private FiniteDuration operationDuration;
    private Timeout operationTimeout;
    private final String selfAddressHostPort;
    private RateLimiter txRateLimiter;
    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final JmxReporter jmxReporter = JmxReporter.forRegistry(metricRegistry).inDomain(DOMAIN).build();
    private final int transactionOutstandingOperationLimit;
    private Timeout transactionCommitOperationTimeout;
    private Timeout shardInitializationTimeout;
    private final Dispatchers dispatchers;

    private volatile SchemaContext schemaContext;
    private volatile boolean updated;

    public ActorContext(ActorSystem actorSystem, ActorRef shardManager,
            ClusterWrapper clusterWrapper, Configuration configuration) {
        this(actorSystem, shardManager, clusterWrapper, configuration,
                DatastoreContext.newBuilder().build());
    }

    public ActorContext(ActorSystem actorSystem, ActorRef shardManager,
            ClusterWrapper clusterWrapper, Configuration configuration,
            DatastoreContext datastoreContext) {
        this.actorSystem = actorSystem;
        this.shardManager = shardManager;
        this.clusterWrapper = clusterWrapper;
        this.configuration = configuration;
        this.datastoreContext = datastoreContext;
        this.dispatchers = new Dispatchers(actorSystem.dispatchers());

        setCachedProperties();

        Address selfAddress = clusterWrapper.getSelfAddress();
        if (selfAddress != null && !selfAddress.host().isEmpty()) {
            selfAddressHostPort = selfAddress.host().get() + ":" + selfAddress.port().get();
        } else {
            selfAddressHostPort = null;
        }

        transactionOutstandingOperationLimit = new CommonConfig(this.getActorSystem().settings().config()).getMailBoxCapacity();
        jmxReporter.start();

    }

    private void setCachedProperties() {
        txRateLimiter = RateLimiter.create(datastoreContext.getTransactionCreationInitialRateLimit());

        operationDuration = Duration.create(datastoreContext.getOperationTimeoutInSeconds(), TimeUnit.SECONDS);
        operationTimeout = new Timeout(operationDuration);

        transactionCommitOperationTimeout =  new Timeout(Duration.create(
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

    public ActorSelection actorSelection(String actorPath) {
        return actorSystem.actorSelection(actorPath);
    }

    public ActorSelection actorSelection(ActorPath actorPath) {
        return actorSystem.actorSelection(actorPath);
    }

    public void setSchemaContext(SchemaContext schemaContext) {
        this.schemaContext = schemaContext;

        if(shardManager != null) {
            shardManager.tell(new UpdateSchemaContext(schemaContext), ActorRef.noSender());
        }
    }

    public void setDatastoreContext(DatastoreContext context) {
        this.datastoreContext = context;
        setCachedProperties();

        // We write the 'updated' volatile to trigger a write memory barrier so that the writes above
        // will be published immediately even though they may not be immediately visible to other
        // threads due to unsynchronized reads. That's OK though - we're going for eventual
        // consistency here as immediately visible updates to these members aren't critical. These
        // members could've been made volatile but wanted to avoid volatile reads as these are
        // accessed often and updates will be infrequent.

        updated = true;

        if(shardManager != null) {
            shardManager.tell(context, ActorRef.noSender());
        }
    }

    public SchemaContext getSchemaContext() {
        return schemaContext;
    }

    public Future<ActorSelection> findPrimaryShardAsync(final String shardName) {
        Future<Object> future = executeOperationAsync(shardManager,
                new FindPrimary(shardName, true), shardInitializationTimeout);

        return future.transform(new Mapper<Object, ActorSelection>() {
            @Override
            public ActorSelection checkedApply(Object response) throws Exception {
                if(response instanceof PrimaryFound) {
                    PrimaryFound found = (PrimaryFound)response;

                    LOG.debug("Primary found {}", found.getPrimaryPath());
                    return actorSystem.actorSelection(found.getPrimaryPath());
                } else if(response instanceof NotInitializedException) {
                    throw (NotInitializedException)response;
                } else if(response instanceof PrimaryNotFoundException) {
                    throw (PrimaryNotFoundException)response;
                } else if(response instanceof NoShardLeaderException) {
                    throw (NoShardLeaderException)response;
                }

                throw new UnknownMessageException(String.format(
                        "FindPrimary returned unkown response: %s", response));
            }
        }, FIND_PRIMARY_FAILURE_TRANSFORMER, getClientDispatcher());
    }

    /**
     * Finds a local shard given its shard name and return it's ActorRef
     *
     * @param shardName the name of the local shard that needs to be found
     * @return a reference to a local shard actor which represents the shard
     *         specified by the shardName
     */
    public Optional<ActorRef> findLocalShard(String shardName) {
        Object result = executeOperation(shardManager, new FindLocalShard(shardName, false));

        if (result instanceof LocalShardFound) {
            LocalShardFound found = (LocalShardFound) result;
            LOG.debug("Local shard found {}", found.getPath());
            return Optional.of(found.getPath());
        }

        return Optional.absent();
    }

    /**
     * Finds a local shard async given its shard name and return a Future from which to obtain the
     * ActorRef.
     *
     * @param shardName the name of the local shard that needs to be found
     */
    public Future<ActorRef> findLocalShardAsync( final String shardName) {
        Future<Object> future = executeOperationAsync(shardManager,
                new FindLocalShard(shardName, true), shardInitializationTimeout);

        return future.map(new Mapper<Object, ActorRef>() {
            @Override
            public ActorRef checkedApply(Object response) throws Throwable {
                if(response instanceof LocalShardFound) {
                    LocalShardFound found = (LocalShardFound)response;
                    LOG.debug("Local shard found {}", found.getPath());
                    return found.getPath();
                } else if(response instanceof NotInitializedException) {
                    throw (NotInitializedException)response;
                } else if(response instanceof LocalShardNotFound) {
                    throw new LocalShardNotFoundException(
                            String.format("Local shard for %s does not exist.", shardName));
                }

                throw new UnknownMessageException(String.format(
                        "FindLocalShard returned unkown response: %s", response));
            }
        }, getClientDispatcher());
    }

    /**
     * Executes an operation on a local actor and wait for it's response
     *
     * @param actor
     * @param message
     * @return The response of the operation
     */
    public Object executeOperation(ActorRef actor, Object message) {
        Future<Object> future = executeOperationAsync(actor, message, operationTimeout);

        try {
            return Await.result(future, operationDuration);
        } catch (Exception e) {
            throw new TimeoutException("Sending message " + message.getClass().toString() +
                    " to actor " + actor.toString() + " failed. Try again later.", e);
        }
    }

    public Future<Object> executeOperationAsync(ActorRef actor, Object message, Timeout timeout) {
        Preconditions.checkArgument(actor != null, "actor must not be null");
        Preconditions.checkArgument(message != null, "message must not be null");

        LOG.debug("Sending message {} to {}", message.getClass(), actor);
        return ask(actor, message, timeout);
    }

    /**
     * Execute an operation on a remote actor and wait for it's response
     *
     * @param actor
     * @param message
     * @return
     */
    public Object executeOperation(ActorSelection actor, Object message) {
        Future<Object> future = executeOperationAsync(actor, message);

        try {
            return Await.result(future, operationDuration);
        } catch (Exception e) {
            throw new TimeoutException("Sending message " + message.getClass().toString() +
                    " to actor " + actor.toString() + " failed. Try again later.", e);
        }
    }

    /**
     * Execute an operation on a remote actor asynchronously.
     *
     * @param actor the ActorSelection
     * @param message the message to send
     * @param timeout the operation timeout
     * @return a Future containing the eventual result
     */
    public Future<Object> executeOperationAsync(ActorSelection actor, Object message,
            Timeout timeout) {
        Preconditions.checkArgument(actor != null, "actor must not be null");
        Preconditions.checkArgument(message != null, "message must not be null");

        LOG.debug("Sending message {} to {}", message.getClass(), actor);

        return ask(actor, message, timeout);
    }

    /**
     * Execute an operation on a remote actor asynchronously.
     *
     * @param actor the ActorSelection
     * @param message the message to send
     * @return a Future containing the eventual result
     */
    public Future<Object> executeOperationAsync(ActorSelection actor, Object message) {
        return executeOperationAsync(actor, message, operationTimeout);
    }

    /**
     * Sends an operation to be executed by a remote actor asynchronously without waiting for a
     * reply (essentially set and forget).
     *
     * @param actor the ActorSelection
     * @param message the message to send
     */
    public void sendOperationAsync(ActorSelection actor, Object message) {
        Preconditions.checkArgument(actor != null, "actor must not be null");
        Preconditions.checkArgument(message != null, "message must not be null");

        LOG.debug("Sending message {} to {}", message.getClass(), actor);

        actor.tell(message, ActorRef.noSender());
    }

    public void shutdown() {
        shardManager.tell(PoisonPill.getInstance(), null);
        actorSystem.shutdown();
    }

    public ClusterWrapper getClusterWrapper() {
        return clusterWrapper;
    }

    public String getCurrentMemberName(){
        return clusterWrapper.getCurrentMemberName();
    }

    /**
     * Send the message to each and every shard
     *
     * @param message
     */
    public void broadcast(final Object message){
        for(final String shardName : configuration.getAllShardNames()){

            Future<ActorSelection> primaryFuture = findPrimaryShardAsync(shardName);
            primaryFuture.onComplete(new OnComplete<ActorSelection>() {
                @Override
                public void onComplete(Throwable failure, ActorSelection primaryShard) {
                    if(failure != null) {
                        LOG.warn("broadcast failed to send message {} to shard {}:  {}",
                                message.getClass().getSimpleName(), shardName, failure);
                    } else {
                        primaryShard.tell(message, ActorRef.noSender());
                    }
                }
            }, getClientDispatcher());
        }
    }

    public FiniteDuration getOperationDuration() {
        return operationDuration;
    }

    public boolean isPathLocal(String path) {
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
     * @deprecated This method is present only to support backward compatibility with Helium and should not be
     * used any further
     *
     *
     * @param primaryPath
     * @param localPathOfRemoteActor
     * @return
    */
    @Deprecated
    public String resolvePath(final String primaryPath,
                                            final String localPathOfRemoteActor) {
        StringBuilder builder = new StringBuilder();
        String[] primaryPathElements = primaryPath.split("/");
        builder.append(primaryPathElements[0]).append("//")
            .append(primaryPathElements[1]).append(primaryPathElements[2]);
        String[] remotePathElements = localPathOfRemoteActor.split("/");
        for (int i = 3; i < remotePathElements.length; i++) {
                builder.append("/").append(remotePathElements[i]);
            }

        return builder.toString();
    }

    /**
     * Get the maximum number of operations that are to be permitted within a transaction before the transaction
     * should begin throttling the operations
     *
     * Parking reading this configuration here because we need to get to the actor system settings
     *
     * @return
     */
    public int getTransactionOutstandingOperationLimit(){
        return transactionOutstandingOperationLimit;
    }

    /**
     * This is a utility method that lets us get a Timer object for any operation. This is a little open-ended to allow
     * us to create a timer for pretty much anything.
     *
     * @param operationName
     * @return
     */
    public Timer getOperationTimer(String operationName){
        final String rate = MetricRegistry.name(DISTRIBUTED_DATA_STORE_METRIC_REGISTRY, datastoreContext.getDataStoreType(), operationName, METRIC_RATE);
        return metricRegistry.timer(rate);
    }

    /**
     * Get the type of the data store to which this ActorContext belongs
     *
     * @return
     */
    public String getDataStoreType() {
        return datastoreContext.getDataStoreType();
    }

    /**
     * Set the number of transaction creation permits that are to be allowed
     *
     * @param permitsPerSecond
     */
    public void setTxCreationLimit(double permitsPerSecond){
        txRateLimiter.setRate(permitsPerSecond);
    }

    /**
     * Get the current transaction creation rate limit
     * @return
     */
    public double getTxCreationLimit(){
        return txRateLimiter.getRate();
    }

    /**
     * Try to acquire a transaction creation permit. Will block if no permits are available.
     */
    public void acquireTxCreationPermit(){
        txRateLimiter.acquire();
    }

    /**
     * Return the operation timeout to be used when committing transactions
     * @return
     */
    public Timeout getTransactionCommitOperationTimeout(){
        return transactionCommitOperationTimeout;
    }

    /**
     * An akka dispatcher that is meant to be used when processing ask Futures which were triggered by client
     * code on the datastore
     * @return
     */
    public ExecutionContext getClientDispatcher() {
        return this.dispatchers.getDispatcher(Dispatchers.DispatcherType.Client);
    }

    public String getNotificationDispatcherPath(){
        return this.dispatchers.getDispatcherPath(Dispatchers.DispatcherType.Notification);
    }

}
