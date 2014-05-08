package org.opendaylight.controller.sal.restconf.rpc.impl;

import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

public abstract class AbstractRpcExecutor implements RpcExecutor {
    private final RpcDefinition rpcDef;

    public AbstractRpcExecutor( RpcDefinition rpcDef ){
        this.rpcDef = rpcDef;
    }

    @Override
    public RpcDefinition getRpcDefinition() {
        return rpcDef;
    }
}