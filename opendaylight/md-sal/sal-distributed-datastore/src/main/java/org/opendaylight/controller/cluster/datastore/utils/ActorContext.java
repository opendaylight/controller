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
import akka.pattern.AskTimeoutException;
import akka.util.Timeout;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.common.actor.CommonConfig;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.Configuration;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException;
import org.opendaylight.controller.cluster.datastore.exceptions.UnknownMessageException;
import org.opendaylight.controller.cluster.datastore.messages.ActorNotInitialized;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryNotFound;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
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
    private static final Logger
        LOG = LoggerFactory.getLogger(ActorContext.class);

    public static final String MAILBOX = "bounded-mailbox";

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

    private final ActorSystem actorSystem;
    private final ActorRef shardManager;
    private final ClusterWrapper clusterWrapper;
    private final Configuration configuration;
    private final DatastoreContext datastoreContext;
    private volatile SchemaContext schemaContext;
    private final FiniteDuration operationDuration;
    private final Timeout operationTimeout;
    private final String selfAddressHostPort;
    private final int transactionOutstandingOperationLimit;

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

        operationDuration = Duration.create(datastoreContext.getOperationTimeoutInSeconds(),
                TimeUnit.SECONDS);
        operationTimeout = new Timeout(operationDuration);

        Address selfAddress = clusterWrapper.getSelfAddress();
        if (selfAddress != null && !selfAddress.host().isEmpty()) {
            selfAddressHostPort = selfAddress.host().get() + ":" + selfAddress.port().get();
        } else {
            selfAddressHostPort = null;
        }

        transactionOutstandingOperationLimit = new CommonConfig(this.getActorSystem().settings().config()).getMailBoxCapacity();
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
            shardManager.tell(new UpdateSchemaContext(schemaContext), null);
        }
    }

    public SchemaContext getSchemaContext() {
        return schemaContext;
    }

    /**
     * Finds the primary shard for the given shard name
     *
     * @param shardName
     * @return
     */
    public Optional<ActorSelection> findPrimaryShard(String shardName) {
        String path = findPrimaryPathOrNull(shardName);
        if (path == null){
            return Optional.absent();
        }
        return Optional.of(actorSystem.actorSelection(path));
    }

    public Future<ActorSelection> findPrimaryShardAsync(final String shardName) {
        Future<Object> future = executeOperationAsync(shardManager,
                new FindPrimary(shardName, true).toSerializable(),
                datastoreContext.getShardInitializationTimeout());

        return future.transform(new Mapper<Object, ActorSelection>() {
            @Override
            public ActorSelection checkedApply(Object response) throws Exception {
                if(response.getClass().equals(PrimaryFound.SERIALIZABLE_CLASS)) {
                    PrimaryFound found = PrimaryFound.fromSerializable(response);

                    LOG.debug("Primary found {}", found.getPrimaryPath());
                    return actorSystem.actorSelection(found.getPrimaryPath());
                } else if(response instanceof ActorNotInitialized) {
                    throw new NotInitializedException(
                            String.format("Found primary shard %s but it's not initialized yet. " +
                                          "Please try again later", shardName));
                } else if(response instanceof PrimaryNotFound) {
                    throw new PrimaryNotFoundException(
                            String.format("No primary shard found for %S.", shardName));
                }

                throw new UnknownMessageException(String.format(
                        "FindPrimary returned unkown response: %s", response));
            }
        }, FIND_PRIMARY_FAILURE_TRANSFORMER, getActorSystem().dispatcher());
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
                new FindLocalShard(shardName, true), datastoreContext.getShardInitializationTimeout());

        return future.map(new Mapper<Object, ActorRef>() {
            @Override
            public ActorRef checkedApply(Object response) throws Throwable {
                if(response instanceof LocalShardFound) {
                    LocalShardFound found = (LocalShardFound)response;
                    LOG.debug("Local shard found {}", found.getPath());
                    return found.getPath();
                } else if(response instanceof ActorNotInitialized) {
                    throw new NotInitializedException(
                            String.format("Found local shard for %s but it's not initialized yet.",
                                    shardName));
                } else if(response instanceof LocalShardNotFound) {
                    throw new LocalShardNotFoundException(
                            String.format("Local shard for %s does not exist.", shardName));
                }

                throw new UnknownMessageException(String.format(
                        "FindLocalShard returned unkown response: %s", response));
            }
        }, getActorSystem().dispatcher());
    }

    private String findPrimaryPathOrNull(String shardName) {
        Object result = executeOperation(shardManager, new FindPrimary(shardName, false).toSerializable());

        if (result.getClass().equals(PrimaryFound.SERIALIZABLE_CLASS)) {
            PrimaryFound found = PrimaryFound.fromSerializable(result);

            LOG.debug("Primary found {}", found.getPrimaryPath());
            return found.getPrimaryPath();

        } else if (result.getClass().equals(ActorNotInitialized.class)){
            throw new NotInitializedException(
                String.format("Found primary shard[%s] but its not initialized yet. Please try again later", shardName)
            );

        } else {
            return null;
        }
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
    public void broadcast(Object message){
        for(String shardName : configuration.getAllShardNames()){

            Optional<ActorSelection> primary = findPrimaryShard(shardName);
            if (primary.isPresent()) {
                primary.get().tell(message, ActorRef.noSender());
            } else {
                LOG.warn("broadcast failed to send message {} to shard {}. Primary not found",
                        message.getClass().getSimpleName(), shardName);
            }
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
}
