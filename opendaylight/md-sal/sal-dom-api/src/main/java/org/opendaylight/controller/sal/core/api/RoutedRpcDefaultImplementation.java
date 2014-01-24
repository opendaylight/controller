package org.opendaylight.controller.sal.core.api;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public interface RoutedRpcDefaultImplementation {

  public RpcResult<CompositeNode> invokeRpc(QName rpc, InstanceIdentifier identifier, CompositeNode input);

}
