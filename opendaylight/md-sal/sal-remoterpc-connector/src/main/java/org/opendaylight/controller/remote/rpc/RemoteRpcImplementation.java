package org.opendaylight.controller.remote.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.sal.core.api.RoutedRpcDefaultImplementation;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.util.Collections;
import java.util.Set;

public class RemoteRpcImplementation implements RpcImplementation,
    RoutedRpcDefaultImplementation {
  @Override
  public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(QName rpc, InstanceIdentifier identifier, CompositeNode input) {
    // TODO : Use RPC Broker to send msg
    return null;
  }

  @Override
  public Set<QName> getSupportedRpcs() {
    // TODO : check if we need to get this from routing registry
    return Collections.emptySet();
  }

  @Override
  public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(QName rpc, CompositeNode input) {
    // TODO : Use RPC broker to send msg
    return null;
  }
}
