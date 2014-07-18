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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.opendaylight.controller.cluster.datastore.Configuration;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
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

    private final ActorSystem actorSystem;
    private final ActorRef shardManager;
    private final Configuration configuration;

    private SchemaContext schemaContext = null;

    public ActorContext(ActorSystem actorSystem, ActorRef shardManager,
        Configuration configuration) {
        this.actorSystem = actorSystem;
        this.shardManager = shardManager;
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

    public String findPrimaryPath(String shardName) {
        Object result = executeLocalOperation(shardManager,
            new FindPrimary(shardName).toSerializable(), ASK_DURATION);

        if (result.getClass().equals(PrimaryFound.SERIALIZABLE_CLASS)) {
            PrimaryFound found = PrimaryFound.fromSerializable(result);

            LOG.error("Primary found {}", found.getPrimaryPath());

            return found.getPrimaryPath();
        }
        throw new PrimaryNotFoundException();
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
            throw new TimeoutException(e);
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
        Future<Object> future =
            ask(actor, message, new Timeout(duration));

        try {
            return Await.result(future, AWAIT_DURATION);
        } catch (Exception e) {
            throw new TimeoutException(e);
        }
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

    public void shutdown() {
        shardManager.tell(PoisonPill.getInstance(), null);
        actorSystem.shutdown();
    }

    public String getRemoteActorPath(final String shardName,
        final String localPathOfRemoteActor) {
        final String path = findPrimaryPath(shardName);

        LoadingCache<String, String> graphs = CacheBuilder.newBuilder()
            .expireAfterAccess(2, TimeUnit.SECONDS)
            .build(
                new CacheLoader<String, String>() {
                    public String load(String key) {
                        return resolvePath(path, localPathOfRemoteActor);
                    }
                }
            );
        return graphs.getUnchecked(localPathOfRemoteActor);
    }

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

}
