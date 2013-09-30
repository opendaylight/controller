package org.opendaylight.controller.sal.binding.spi;

import java.util.Map;

import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

public interface RpcRoutingTable<C extends BaseIdentity,S extends RpcService> {

    Class<C> getContextIdentifier();
    
    /**
     * Updates route for particular path to specified instance of {@link RpcService}. 
     * 
     * @param path Path for which RpcService routing is to be updated
     * @param service Instance of RpcService which is responsible for processing Rpc Requests.
     */
    void updateRoute(InstanceIdentifier path,S service);
    
    /**
     * Deletes a route for particular path
     * 
     * @param path Path for which 
     */
    void deleteRoute(InstanceIdentifier path);
    
    /**
     * 
     */
    S getService(InstanceIdentifier nodeInstance);
    
    /**
     * 
     * @return
     */
    Map<InstanceIdentifier,S> getRoutes();
}
