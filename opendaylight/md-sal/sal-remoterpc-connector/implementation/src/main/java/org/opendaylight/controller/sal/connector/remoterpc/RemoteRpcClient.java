package org.opendaylight.controller.sal.connector.remoterpc;

import org.opendaylight.controller.sal.core.api.RpcImplementation;

public interface RemoteRpcClient extends RpcImplementation,AutoCloseable{


    void setRoutingTableProvider(RoutingTableProvider provider);
    
    void stop();
    
    void start();
}
