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
public interface RpcRouter<C,T,R,D> {

    
    
    Future<RpcReply<D>> sendRpc(RpcRequest<C, T, R, D> input);
    
    
    /**
     * 
     * @author 
     *
     * @param <C> Routing Context Identifier
        * @param <R> Route Type
        * @param <T> Rpc Type
        * @param <D> Data Type
     */
    public interface RpcRequest<C,T,R,D> {

        RouteIdentifier<C,T,R> getRoutingInformation();
        D getPayload();
    }
    
    public interface RouteIdentifier<C,T,R> {
        
        C getContext(); // defines a routing table (e.g. NodeContext)
        T getType(); // rpc type
        R getRoute(); // e.g. (node identity)
    }
    
    public interface RpcReply<D> {
        D getPayload();
    }
}
