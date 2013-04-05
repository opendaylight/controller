/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.spi;

import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.yang.binding.DataObject;
import org.opendaylight.controller.yang.binding.RpcService;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.common.RpcResult;

public interface RpcMapper<T extends RpcService> {
    
    Set<QName> getRpcQNames();
    
    /**
     * Returns a class object representing subinterface
     * to whom, this mapper is assigned.
     * 
     * @return
     */
    Class<T> getServiceClass();
    
    /**
     * Returns a Binding Mapper for Rpc Input Data
     * @return
     */
    Mapper<?> getInputMapper();
    /**
     * Returns a Binding Mapper for Rpc Output Data
     * 
     * @return
     */
    Mapper<?> getOutputMapper();
    
    /**
     * Returns a consumer proxy, which is responsible
     * for invoking the rpc functionality of {@link BindingAwareBroker} implementation.
     * 
     * @return Proxy of {@link RpcService} assigned to this mapper.
     */
    T getConsumerProxy(RpcProxyInvocationHandler handler);
    
    /**
     * Invokes the method of RpcService representing the supplied rpc.
     * 
     * @param rpc QName of Rpc
     * @param impl Implementation of RpcService on which the method should be invoked
     * @param baInput Input Data to RPC method
     * @return Result of RPC invocation.
     */
    RpcResult<? extends DataObject> invokeRpcImplementation(QName rpc,
            RpcService impl, DataObject baInput);
    
    public interface RpcProxyInvocationHandler {
        
        Future<RpcResult<? extends DataObject>> invokeRpc(RpcService proxy, QName rpc, DataObject input);
    }
}
