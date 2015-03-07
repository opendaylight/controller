/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertNotNull;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;

public class MockActorContext extends ActorContext {

    private volatile Object executeShardOperationResponse;
    private volatile Object executeRemoteOperationResponse;
    private volatile Object executeLocalOperationResponse;
    private volatile Object executeLocalShardOperationResponse;
    private volatile Exception executeRemoteOperationFailure;
    private volatile Object inputMessage;

    public MockActorContext(ActorSystem actorSystem) {
        super(actorSystem, null, new MockClusterWrapper(), new MockConfiguration());
    }

    public MockActorContext(ActorSystem actorSystem, ActorRef shardManager) {
        super(actorSystem, shardManager, new MockClusterWrapper(), new MockConfiguration());
    }

    @Override public Object executeOperation(ActorSelection actor,
                                             Object message) {
        return executeRemoteOperationResponse;
    }

    public void setExecuteShardOperationResponse(Object response){
        executeShardOperationResponse = response;
    }

    public void setExecuteRemoteOperationResponse(Object response){
        executeRemoteOperationResponse = response;
    }

    public void setExecuteRemoteOperationFailure(Exception executeRemoteOperationFailure) {
        this.executeRemoteOperationFailure = executeRemoteOperationFailure;
    }

    public void setExecuteLocalOperationResponse(
        Object executeLocalOperationResponse) {
        this.executeLocalOperationResponse = executeLocalOperationResponse;
    }

    public void setExecuteLocalShardOperationResponse(
        Object executeLocalShardOperationResponse) {
        this.executeLocalShardOperationResponse = executeLocalShardOperationResponse;
    }

    @SuppressWarnings("unchecked")
    public <T> T getInputMessage(Class<T> expType) throws Exception {
        assertNotNull("Input message was null", inputMessage);
        return (T) expType.getMethod("fromSerializable", Object.class).invoke(null, inputMessage);
    }

    @Override
    public Object executeOperation(ActorRef actor,
                                   Object message) {
        return this.executeLocalOperationResponse;
    }

}
