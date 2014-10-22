/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.util.Timeout;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.Configuration;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException;
import org.opendaylight.controller.cluster.datastore.messages.ActorNotInitialized;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

/**
 * The ActorContext class contains utility methods which could be used by
 * non-actors (like DistributedDataStore) to work with actors a little more
 * easily. An ActorContext can be freely passed around to local object instances
 * but should not be passed to actors especially remote actors
 */
public class ActorContext {
    private static final Logger
        LOG = LoggerFactory.getLogger(ActorContext.class);

    private static final FiniteDuration DEFAULT_OPER_DURATION = Duration.create(5, TimeUnit.SECONDS);

    public static final String MAILBOX = "bounded-mailbox";

    private final ActorSystem actorSystem;
    private final ActorRef shardManager;
    private final ClusterWrapper clusterWrapper;
    private final Configuration configuration;
    private volatile SchemaContext schemaContext;
    private FiniteDuration operationDuration = DEFAULT_OPER_DURATION;
    private Timeout operationTimeout = new Timeout(operationDuration);

    public ActorContext(ActorSystem actorSystem, ActorRef shardManager,
        ClusterWrapper clusterWrapper,
        Configuration configuration) {
        this.actorSystem = actorSystem;
        this.shardManager = shardManager;
        this.clusterWrapper = clusterWrapper;
        this.configuration = configuration;
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

    public void setOperationTimeout(int timeoutInSeconds) {
        operationDuration = Duration.create(timeoutInSeconds, TimeUnit.SECONDS);
        operationTimeout = new Timeout(operationDuration);
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

    /**
     * Finds a local shard given it's shard name and return it's ActorRef
     *
     * @param shardName the name of the local shard that needs to be found
     * @return a reference to a local shard actor which represents the shard
     *         specified by the shardName
     */
    public Optional<ActorRef> findLocalShard(String shardName) {
        Object result = executeOperation(shardManager, new FindLocalShard(shardName));

        if (result instanceof LocalShardFound) {
            LocalShardFound found = (LocalShardFound) result;
            LOG.debug("Local shard found {}", found.getPath());
            return Optional.of(found.getPath());
        }

        return Optional.absent();
    }


    private String findPrimaryPathOrNull(String shardName) {
        Object result = executeOperation(shardManager, new FindPrimary(shardName).toSerializable());

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

        LOG.debug("Sending message {} to {}", message.getClass().toString(), actor.toString());
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
     * @return a Future containing the eventual result
     */
    public Future<Object> executeOperationAsync(ActorSelection actor, Object message) {
        Preconditions.checkArgument(actor != null, "actor must not be null");
        Preconditions.checkArgument(message != null, "message must not be null");

        LOG.debug("Sending message {} to {}", message.getClass().toString(), actor.toString());

        return ask(actor, message, operationTimeout);
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

        LOG.debug("Sending message {} to {}", message.getClass().toString(), actor.toString());

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
}
