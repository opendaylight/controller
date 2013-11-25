package org.opendaylight.controller.sal.binding.codegen.impl

import org.opendaylight.controller.sal.binding.spi.RpcRoutingTable
import org.opendaylight.yangtools.yang.binding.BaseIdentity
import org.opendaylight.yangtools.yang.binding.RpcService
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.Map
import org.opendaylight.yangtools.yang.binding.DataObject
import java.util.HashMap

class RpcRoutingTableImpl<C extends BaseIdentity,S extends RpcService> implements RpcRoutingTable<C,S>{
    
    @Property
    val Class<C> identifier;
    
    @Property
    var S defaultRoute;
    
    @Property
    val Map<InstanceIdentifier<? extends DataObject>,S> routes;
    
    new(Class<C> ident, Map<InstanceIdentifier<? extends DataObject>,S> route) {
        _identifier = ident
        _routes = route
    }
    
    new(Class<C> ident) {
        _identifier = ident
        _routes = new HashMap
    }
    
    
    override getRoute(InstanceIdentifier<? extends Object> nodeInstance) {
        val ret = routes.get(nodeInstance);
        if(ret !== null) {
            return ret;
        }
        return defaultRoute;
    }
    
    override removeRoute(InstanceIdentifier<? extends Object> path) {
        routes.remove(path);
    }
    
    @SuppressWarnings("rawtypes")
    override updateRoute(InstanceIdentifier<? extends Object> path, S service) {
        routes.put(path as InstanceIdentifier<? extends DataObject>,service);
    }
}