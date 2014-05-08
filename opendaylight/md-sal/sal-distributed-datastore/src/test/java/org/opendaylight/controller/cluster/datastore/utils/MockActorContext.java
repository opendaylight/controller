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
import scala.concurrent.duration.FiniteDuration;

public class MockActorContext extends ActorContext {

    private Object executeShardOperationResponse;
    private Object executeRemoteOperationResponse;
    private Object executeLocalOperationResponse;

    public MockActorContext(ActorSystem actorSystem) {
        super(actorSystem, null);
    }

    public MockActorContext(ActorSystem actorSystem, ActorRef shardManager) {
        super(actorSystem, shardManager);
    }


    @Override public Object executeShardOperation(String shardName,
        Object message, FiniteDuration duration) {
        return executeShardOperationResponse;
    }

    @Override public Object executeRemoteOperation(ActorSelection actor,
        Object message, FiniteDuration duration) {
        return executeRemoteOperationResponse;
    }

    @Override public ActorSelection findPrimary(String shardName) {
        return null;
    }

    public void setExecuteShardOperationResponse(Object response){
        executeShardOperationResponse = response;
    }

    public void setExecuteRemoteOperationResponse(Object response){
        executeRemoteOperationResponse = response;
    }

    public void setExecuteLocalOperationResponse(
        Object executeLocalOperationResponse) {
        this.executeLocalOperationResponse = executeLocalOperationResponse;
    }


}
