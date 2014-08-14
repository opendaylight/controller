/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;


import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import org.opendaylight.controller.cluster.datastore.exceptions.OperationException;
import org.opendaylight.controller.cluster.datastore.exceptions.RemoteOperationException;
import scala.concurrent.duration.FiniteDuration;

public class MockActorContext extends ActorContext {

    private Object executeShardOperationResponse;
    private Object executeRemoteOperationResponse;
    private Object executeLocalOperationResponse;
    private Object executeLocalShardOperationResponse;
    private Object remoteOperationResponseOnExecution;

    public MockActorContext(ActorSystem actorSystem) {
        super(actorSystem, null, new MockClusterWrapper(),
            new MockConfiguration());
    }

    public MockActorContext(ActorSystem actorSystem, ActorRef shardManager) {
        super(actorSystem, shardManager, new MockClusterWrapper(),
            new MockConfiguration());
    }


    @Override public Object executeShardOperation(String shardName,
        Object message, FiniteDuration duration)
        throws RemoteOperationException {
        return executeShardOperationResponse;
    }

    @Override public Object executeRemoteOperation(ActorSelection actor,
        Object message, FiniteDuration duration)
        throws RemoteOperationException {
        remoteOperationResponseOnExecution = executeRemoteOperationResponse;
        return executeRemoteOperationResponse;
    }

    @Override public ActorSelection findPrimary(String shardName) {
        return null;
    }

    public void setExecuteShardOperationResponse(Object response) {
        executeShardOperationResponse = response;
    }

    public void setExecuteRemoteOperationResponse(Object response) {
        executeRemoteOperationResponse = response;
    }

    public void setExecuteLocalOperationResponse(
        Object executeLocalOperationResponse) {
        this.executeLocalOperationResponse = executeLocalOperationResponse;
    }

    public void setExecuteLocalShardOperationResponse(
        Object executeLocalShardOperationResponse) {
        this.executeLocalShardOperationResponse =
            executeLocalShardOperationResponse;
    }

    @Override public Object executeLocalOperation(ActorRef actor,
        Object message, FiniteDuration duration) throws OperationException {
        return this.executeLocalOperationResponse;
    }

    @Override public Object executeLocalShardOperation(String shardName,
        Object message, FiniteDuration duration) {
        return this.executeLocalShardOperationResponse;
    }

    //for unit test cases purposes

    public Object getRemoteOperationResponseOnExecution() {
        return remoteOperationResponseOnExecution;

    }
}
