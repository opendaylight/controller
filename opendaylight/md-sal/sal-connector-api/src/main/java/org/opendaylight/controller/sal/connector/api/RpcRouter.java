/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
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
    interface RpcRequest<C,T,R,D> {

        RouteIdentifier<C,T,R> getRoutingInformation();
        D getPayload();
    }

    interface RouteIdentifier<C,T,R> {

        C getContext(); // defines a routing table (e.g. NodeContext)
        T getType(); // rpc type
        R getRoute(); // e.g. (node identity)
    }

    interface RpcReply<D> {
        D getPayload();
    }
}
