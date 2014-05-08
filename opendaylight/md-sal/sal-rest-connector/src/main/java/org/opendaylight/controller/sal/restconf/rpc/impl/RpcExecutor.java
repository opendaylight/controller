package org.opendaylight.controller.sal.restconf.rpc.impl;

import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

public interface RpcExecutor {
    RpcResult<CompositeNode> invokeRpc( CompositeNode rpcRequest );

    RpcDefinition getRpcDefinition();
}