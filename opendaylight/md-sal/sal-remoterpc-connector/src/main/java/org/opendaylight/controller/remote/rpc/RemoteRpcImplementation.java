package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.remote.rpc.messages.ErrorResponse;
import org.opendaylight.controller.remote.rpc.messages.InvokeRoutedRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.api.RoutedRpcDefaultImplementation;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class RemoteRpcImplementation implements RpcImplementation,
    RoutedRpcDefaultImplementation {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteRpcImplementation.class);
  private ActorRef rpcBroker;

  public RemoteRpcImplementation(ActorRef rpcBroker) {
    this.rpcBroker = rpcBroker;
  }

  @Override
  public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(QName rpc, InstanceIdentifier identifier, CompositeNode input) {
    InvokeRoutedRpc rpcMsg = new InvokeRoutedRpc(rpc, identifier, input);
    CompositeNode result = null;
    Collection<RpcError> errors = null;
    try {
      Object response = ActorUtil.executeLocalOperation(rpcBroker, rpcMsg, ActorUtil.ASK_DURATION);
      if(response instanceof RpcResponse) {
        RpcResponse rpcResponse = (RpcResponse) response;
        result = rpcResponse.getResult();
      } else if(response instanceof ErrorResponse) {
        ErrorResponse errorResponse = (ErrorResponse) response;
        errors = errorResponse.getErrors();
      }
    } catch (Exception e) {
      LOG.error("Error occurred while invoking RPC actor {}", e.toString());
    }

    return Futures.immediateFuture(Rpcs.getRpcResult(true, result, errors));
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
