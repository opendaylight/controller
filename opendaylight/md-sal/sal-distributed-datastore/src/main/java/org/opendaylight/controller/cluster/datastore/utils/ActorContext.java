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

import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.Configuration;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException;
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

    public static final FiniteDuration ASK_DURATION =
        Duration.create(5, TimeUnit.SECONDS);
    public static final Duration AWAIT_DURATION =
        Duration.create(5, TimeUnit.SECONDS);

    public static final String MAILBOX = "bounded-mailbox";

    private final ActorSystem actorSystem;
    private final ActorRef shardManager;
    private final ClusterWrapper clusterWrapper;
    private final Configuration configuration;
    private volatile SchemaContext schemaContext;

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

    public SchemaContext getSchemaContext() {
        return schemaContext;
    }

    /**
     * Finds the primary for a given shard
     *
     * @param shardName
     * @return
     */
    public ActorSelection findPrimary(String shardName) {
        String path = findPrimaryPath(shardName);
        return actorSystem.actorSelection(path);
    }

    /**
     * Finds a local shard given it's shard name and return it's ActorRef
     *
     * @param shardName the name of the local shard that needs to be found
     * @return a reference to a local shard actor which represents the shard
     *         specified by the shardName
     */
    public ActorRef findLocalShard(String shardName) {
        Object result = executeLocalOperation(shardManager,
            new FindLocalShard(shardName), ASK_DURATION);

        if (result instanceof LocalShardFound) {
            LocalShardFound found = (LocalShardFound) result;

            LOG.debug("Local shard found {}", found.getPath());

            return found.getPath();
        }

        return null;
    }


    public String findPrimaryPath(String shardName) {
        Object result = executeLocalOperation(shardManager,
            new FindPrimary(shardName).toSerializable(), ASK_DURATION);

        if (result.getClass().equals(PrimaryFound.SERIALIZABLE_CLASS)) {
            PrimaryFound found = PrimaryFound.fromSerializable(result);

            LOG.debug("Primary found {}", found.getPrimaryPath());

            return found.getPrimaryPath();
        }
        throw new PrimaryNotFoundException("Could not find primary for shardName " + shardName);
    }


    /**
     * Executes an operation on a local actor and wait for it's response
     *
     * @param actor
     * @param message
     * @param duration
     * @return The response of the operation
     */
    public Object executeLocalOperation(ActorRef actor, Object message,
        FiniteDuration duration) {
        Future<Object> future =
            ask(actor, message, new Timeout(duration));

        try {
            return Await.result(future, AWAIT_DURATION);
        } catch (Exception e) {
            throw new TimeoutException("Sending message " + message.getClass().toString() + " to actor " + actor.toString() + " failed" , e);
        }
    }

    /**
     * Execute an operation on a remote actor and wait for it's response
     *
     * @param actor
     * @param message
     * @param duration
     * @return
     */
    public Object executeRemoteOperation(ActorSelection actor, Object message,
        FiniteDuration duration) {

        LOG.debug("Sending remote message {} to {}", message.getClass().toString(), actor.toString());

        Future<Object> future =
            ask(actor, message, new Timeout(duration));

        try {
            return Await.result(future, AWAIT_DURATION);
        } catch (Exception e) {
            throw new TimeoutException("Sending message " + message.getClass().toString() + " to actor " + actor.toString() + " failed" , e);
        }
    }

    /**
     * Execute an operation on a remote actor asynchronously.
     *
     * @param actor the ActorSelection
     * @param message the message to send
     * @param duration the maximum amount of time to send he message
     * @return a Future containing the eventual result
     */
    public Future<Object> executeRemoteOperationAsync(ActorSelection actor, Object message,
            FiniteDuration duration) {

        LOG.debug("Sending remote message {} to {}", message.getClass().toString(), actor.toString());

        return ask(actor, message, new Timeout(duration));
    }

    /**
     * Sends an operation to be executed by a remote actor asynchronously without waiting for a
     * reply (essentially set and forget).
     *
     * @param actor the ActorSelection
     * @param message the message to send
     */
    public void sendRemoteOperationAsync(ActorSelection actor, Object message) {
        actor.tell(message, ActorRef.noSender());
    }

    /**
     * Execute an operation on the primary for a given shard
     * <p>
     * This method first finds the primary for a given shard ,then sends
     * the message to the remote shard and waits for a response
     * </p>
     *
     * @param shardName
     * @param message
     * @param duration
     * @return
     * @throws org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException         if the message to the remote shard times out
     * @throws org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException if the primary shard is not found
     */
    public Object executeShardOperation(String shardName, Object message,
        FiniteDuration duration) {
        ActorSelection primary = findPrimary(shardName);

        return executeRemoteOperation(primary, message, duration);
    }

    /**
     * Execute an operation on the the local shard only
     * <p>
     *     This method first finds the address of the local shard if any. It then
     *     executes the operation on it.
     * </p>
     *
     * @param shardName the name of the shard on which the operation needs to be executed
     * @param message the message that needs to be sent to the shard
     * @param duration the time duration in which this operation should complete
     * @return the message that was returned by the local actor on which the
     *         the operation was executed. If a local shard was not found then
     *         null is returned
     * @throws org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException
     *         if the operation does not complete in a specified time duration
     */
    public Object executeLocalShardOperation(String shardName, Object message,
        FiniteDuration duration) {
        ActorRef local = findLocalShard(shardName);

        if(local != null) {
            return executeLocalOperation(local, message, duration);
        }

        return null;
    }


    public void shutdown() {
        shardManager.tell(PoisonPill.getInstance(), null);
        actorSystem.shutdown();
    }

    /**
     * @deprecated Need to stop using this method. There are ways to send a
     * remote ActorRef as a string which should be used instead of this hack
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

    public ActorPath actorFor(String path){
        return actorSystem.actorFor(path).path();
    }

    public String getCurrentMemberName(){
        return clusterWrapper.getCurrentMemberName();
    }

}
