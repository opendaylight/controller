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