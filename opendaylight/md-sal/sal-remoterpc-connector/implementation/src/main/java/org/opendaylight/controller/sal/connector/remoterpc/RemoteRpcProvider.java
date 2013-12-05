package org.opendaylight.controller.sal.connector.remoterpc;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.sal.connector.remoterpc.Client;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider.ProviderFunctionality;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public class RemoteRpcProvider implements 
    RemoteRpcServer,
    RemoteRpcClient,
    ProviderFunctionality{

    private final ServerImpl server;
    private final Client client;
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
    
    
    public RemoteRpcProvider(ServerImpl server, Client client) {
        this.server = server;
        this.client = client;
    }
    
    public void setBrokerSession(ProviderSession session) {
        server.setBrokerSession(session);
    }
    public void setServerPool(ExecutorService serverPool) {
        server.setServerPool(serverPool);
    }
    public void start() {
        client.setRoutingTableProvider(provider);
        server.setRoutingTableProvider(provider);
        server.start();
        client.start();
    }
    public void onRouteUpdated(String key, Set values) {
        server.onRouteUpdated(key, values);
    }
    public void onRouteDeleted(String key) {
        server.onRouteDeleted(key);
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
