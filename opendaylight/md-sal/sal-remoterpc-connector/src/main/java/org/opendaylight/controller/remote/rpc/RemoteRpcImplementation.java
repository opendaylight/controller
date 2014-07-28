package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.remote.rpc.messages.ErrorResponse;
import org.opendaylight.controller.remote.rpc.messages.InvokeRoutedRpc;
import org.opendaylight.controller.remote.rpc.messages.InvokeRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.controller.sal.common.util.RpcErrors;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.api.RoutedRpcDefaultImplementation;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class RemoteRpcImplementation implements RpcImplementation,
    RoutedRpcDefaultImplementation {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteRpcImplementation.class);
  private ActorRef rpcBroker;
  private SchemaContext schemaContext;

  public RemoteRpcImplementation(ActorRef rpcBroker, SchemaContext schemaContext) {
    this.rpcBroker = rpcBroker;
    this.schemaContext = schemaContext;
  }

  @Override
  public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(QName rpc, InstanceIdentifier identifier, CompositeNode input) {
    InvokeRoutedRpc rpcMsg = new InvokeRoutedRpc(rpc, identifier, input);

    return executeMsg(rpcMsg);
  }

  @Override
  public Set<QName> getSupportedRpcs() {
    // TODO : check if we need to get this from routing registry
    return Collections.emptySet();
  }

  @Override
  public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(QName rpc, CompositeNode input) {
    InvokeRpc rpcMsg = new InvokeRpc(rpc, input);
    return executeMsg(rpcMsg);
  }

  private ListenableFuture<RpcResult<CompositeNode>> executeMsg(Object rpcMsg) {
    CompositeNode result = null;
    Collection<RpcError> errors = errors = new ArrayList<>();
    try {
      Object response = ActorUtil.executeLocalOperation(rpcBroker, rpcMsg, ActorUtil.ASK_DURATION, ActorUtil.AWAIT_DURATION);
      if(response instanceof RpcResponse) {
        RpcResponse rpcResponse = (RpcResponse) response;
        result = XmlUtils.xmlToCompositeNode(rpcResponse.getResultCompositeNode());
      } else if(response instanceof ErrorResponse) {
        ErrorResponse errorResponse = (ErrorResponse) response;
        Exception e = errorResponse.getException();
        errors.add(RpcErrors.getRpcError(null, null, null, null, e.getMessage(), null, e.getCause()));
      }
    } catch (Exception e) {
      LOG.error("Error occurred while invoking RPC actor {}", e.toString());
      errors.add(RpcErrors.getRpcError(null, null, null, null, e.getMessage(), null, e.getCause()));
    }
    return Futures.immediateFuture(Rpcs.getRpcResult(true, result, errors));
  }
}
