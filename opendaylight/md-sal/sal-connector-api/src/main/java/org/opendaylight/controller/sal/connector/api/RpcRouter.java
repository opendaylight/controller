package org.opendaylight.controller.sal.connector.api;

import java.util.concurrent.Future;

/**
 * 
 * @author ttkacik
 *
 * @param <C> Routing Context Identifier
 * @param <R> Route Type
 * @param <T> Rpc Type
 * @param <D> Data Type
 */
public interface RpcRouter<C,R,T,D> {

    
    
    Future<RpcReply<D>> sendRpc(RpcRequest<C, R, T, D> input);
    
    
    /**
     * 
     * @author 
     *
     * @param <C> Routing Context Identifier
        * @param <R> Route Type
        * @param <T> Rpc Type
        * @param <D> Data Type
     */
    public interface RpcRequest<C,R,T,D> {

        RouteIdentifier<C,R,T> getRoutingInformation();
        D getPayload();
    }
    
    public interface RouteIdentifier<C,R,T> {
        
        C getContext(); // defines a routing table (e.g. NodeContext)
        R getRoute(); // e.g. (node identity)
        T getType(); // rpc type
    }
    
    public interface RpcReply<D> {
        D getPayload();
    }
}
