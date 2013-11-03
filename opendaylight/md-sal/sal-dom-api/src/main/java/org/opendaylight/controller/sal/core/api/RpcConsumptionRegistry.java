package org.opendaylight.controller.sal.core.api;

import java.util.concurrent.Future;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public interface RpcConsumptionRegistry {
    /**
     * Sends an RPC to other components registered to the broker.
     * 
     * @see RpcImplementation
     * @param rpc
     *            Name of RPC
     * @param input
     *            Input data to the RPC
     * @return Result of the RPC call
     */
    Future<RpcResult<CompositeNode>> rpc(QName rpc, CompositeNode input);

}
