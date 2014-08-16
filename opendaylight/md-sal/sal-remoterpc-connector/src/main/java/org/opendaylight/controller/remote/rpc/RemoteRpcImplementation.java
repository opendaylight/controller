package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.remote.rpc.messages.ErrorResponse;
import org.opendaylight.controller.remote.rpc.messages.InvokeRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.controller.remote.rpc.utils.ActorUtil;
import org.opendaylight.controller.xml.codec.XmlUtils;
import org.opendaylight.controller.sal.core.api.RoutedRpcDefaultImplementation;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(QName rpc, YangInstanceIdentifier identifier, CompositeNode input) {
    InvokeRpc rpcMsg = new InvokeRpc(rpc, identifier, input);

    return executeMsg(rpcMsg);
  }

  @Override
  public Set<QName> getSupportedRpcs() {
    // TODO : check if we need to get this from routing registry
    return Collections.emptySet();
  }

  @Override
  public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(QName rpc, CompositeNode input) {
    InvokeRpc rpcMsg = new InvokeRpc(rpc, null, input);
    return executeMsg(rpcMsg);
  }

  private ListenableFuture<RpcResult<CompositeNode>> executeMsg(Object rpcMsg) {
    ListenableFuture<RpcResult<CompositeNode>> listenableFuture = null;

    try {
      Object response = ActorUtil.executeOperation(rpcBroker, rpcMsg, ActorUtil.ASK_DURATION, ActorUtil.AWAIT_DURATION);
      if(response instanceof RpcResponse) {

        RpcResponse rpcResponse = (RpcResponse) response;
        CompositeNode result = XmlUtils.xmlToCompositeNode(rpcResponse.getResultCompositeNode());
        listenableFuture = Futures.immediateFuture(RpcResultBuilder.success(result).build());

      } else if(response instanceof ErrorResponse) {

        ErrorResponse errorResponse = (ErrorResponse) response;
        Exception e = errorResponse.getException();
        final RpcResultBuilder<CompositeNode> failed = RpcResultBuilder.failed();
        failed.withError(null, null, e.getMessage(), null, null, e.getCause());
        listenableFuture = Futures.immediateFuture(failed.build());

      }
    } catch (Exception e) {
      LOG.error("Error occurred while invoking RPC actor {}", e);

      final RpcResultBuilder<CompositeNode> failed = RpcResultBuilder.failed();
      failed.withError(null, null, e.getMessage(), null, null, e.getCause());
      listenableFuture = Futures.immediateFuture(failed.build());
    }

    return listenableFuture;
  }
}
