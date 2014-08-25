package org.opendaylight.controller.remote.rpc;

import static akka.pattern.Patterns.ask;
import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.util.Timeout;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.opendaylight.controller.remote.rpc.messages.InvokeRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.controller.remote.rpc.utils.ActorUtil;
import org.opendaylight.controller.xml.codec.XmlUtils;
import org.opendaylight.controller.sal.core.api.RoutedRpcDefaultImplementation;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.concurrent.ExecutionContext;

import java.util.Collections;
import java.util.Set;

public class RemoteRpcImplementation implements RpcImplementation,
RoutedRpcDefaultImplementation {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteRpcImplementation.class);
    private final ActorRef rpcBroker;
    private final SchemaContext schemaContext;

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

    private ListenableFuture<RpcResult<CompositeNode>> executeMsg(InvokeRpc rpcMsg) {

        final SettableFuture<RpcResult<CompositeNode>> listenableFuture = SettableFuture.create();

        scala.concurrent.Future<Object> future = ask(rpcBroker, rpcMsg,
                new Timeout(ActorUtil.ASK_DURATION));

        OnComplete<Object> onComplete = new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object reply) throws Throwable {
                if(failure != null) {
                    LOG.error("InvokeRpc failed", failure);

                    RpcResult<CompositeNode> rpcResult;
                    if(failure instanceof RpcErrorsException) {
                        rpcResult = RpcResultBuilder.<CompositeNode>failed().withRpcErrors(
                                ((RpcErrorsException)failure).getRpcErrors()).build();
                    } else {
                        rpcResult = RpcResultBuilder.<CompositeNode>failed().withError(
                                ErrorType.RPC, failure.getMessage(), failure).build();
                    }

                    listenableFuture.set(rpcResult);
                    return;
                }

                RpcResponse rpcReply = (RpcResponse)reply;
                CompositeNode result = XmlUtils.xmlToCompositeNode(rpcReply.getResultCompositeNode());
                listenableFuture.set(RpcResultBuilder.success(result).build());
            }
        };

        future.onComplete(onComplete, ExecutionContext.Implicits$.MODULE$.global());

        //    try {
        //      Object response = ActorUtil.executeOperation(rpcBroker, rpcMsg, ActorUtil.ASK_DURATION, ActorUtil.AWAIT_DURATION);
        //      if(response instanceof RpcResponse) {
        //
        //        RpcResponse rpcResponse = (RpcResponse) response;
        //        CompositeNode result = XmlUtils.xmlToCompositeNode(rpcResponse.getResultCompositeNode());
        //        listenableFuture = Futures.immediateFuture(RpcResultBuilder.success(result).build());
        //
        //      } else if(response instanceof ErrorResponse) {
        //
        //        ErrorResponse errorResponse = (ErrorResponse) response;
        //        Exception e = errorResponse.getException();
        //        final RpcResultBuilder<CompositeNode> failed = RpcResultBuilder.failed();
        //        failed.withError(null, null, e.getMessage(), null, null, e.getCause());
        //        listenableFuture = Futures.immediateFuture(failed.build());
        //
        //      }
        //    } catch (Exception e) {
        //      LOG.error("Error occurred while invoking RPC actor {}", e);
        //
        //      final RpcResultBuilder<CompositeNode> failed = RpcResultBuilder.failed();
        //      failed.withError(null, null, e.getMessage(), null, null, e.getCause());
        //      listenableFuture = Futures.immediateFuture(failed.build());
        //    }

        return listenableFuture;
    }
}
