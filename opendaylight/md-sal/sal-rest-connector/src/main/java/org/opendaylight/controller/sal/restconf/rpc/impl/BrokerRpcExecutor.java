/*
* Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.sal.restconf.rpc.impl;

import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

public class BrokerRpcExecutor extends AbstractRpcExecutor {
    private final BrokerFacade broker;

    public BrokerRpcExecutor( RpcDefinition rpcDef, BrokerFacade broker )
    {
        super( rpcDef );
        this.broker = broker;
    }

    @Override
    public RpcResult<CompositeNode> invokeRpc(CompositeNode rpcRequest) {
        return broker.invokeRpc( getRpcDefinition().getQName(), rpcRequest );
    }
}