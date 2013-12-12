/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

import java.util.Collection;
import java.util.Set;

public class RemoteRpcProvider implements 
    RemoteRpcServer,
    RemoteRpcClient,
    Provider {

    private final ServerImpl server;
    private final ClientImpl client;
    private RoutingTableProvider provider;

    @Override
    public void setRoutingTableProvider(RoutingTableProvider provider) {
        this.provider = provider;
        server.setRoutingTableProvider(provider);
        client.setRoutingTableProvider(provider);
    }
    
    @Override
    public RpcResult<CompositeNode> invokeRpc(QName rpc, CompositeNode input) {
        return client.invokeRpc(rpc, input);
    }
    
    @Override
    public Set<QName> getSupportedRpcs() {
        return client.getSupportedRpcs();
    }
    
    
    public RemoteRpcProvider(ServerImpl server, ClientImpl client) {
        this.server = server;
        this.client = client;
    }
    
    public void setBrokerSession(ProviderSession session) {
        server.setBrokerSession(session);
    }
//    public void setServerPool(ExecutorService serverPool) {
//        server.setServerPool(serverPool);
//    }
    public void start() {
        //when listener was being invoked and addRPCImplementation was being
        //called the client was null.
        server.setClient(client);
        server.start();
        client.start();


    }

    
    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        // TODO Auto-generated method stub
        return null;
    }
    
    
    @Override
    public void onSessionInitiated(ProviderSession session) {
        server.setBrokerSession(session);
        start();
    }
    
    
    public void close() throws Exception {
        server.close();
        client.close();
    }

    @Override
    public void stop() {
        server.stop();
        client.stop();
    }
}
