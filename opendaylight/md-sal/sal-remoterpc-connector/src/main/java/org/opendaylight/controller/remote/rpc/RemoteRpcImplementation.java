package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.util.Timeout;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.remote.rpc.messages.InvokeRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.controller.sal.core.api.RoutedRpcDefaultImplementation;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.xml.codec.XmlUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;

import java.util.Collections;
import java.util.Set;

import static akka.pattern.Patterns.ask;

public class RemoteRpcImplementation implements RpcImplementation, RoutedRpcDefaultImplementation {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteRpcImplementation.class);
    private final ActorRef rpcBroker;
    private final SchemaContext schemaContext;
    private final RemoteRpcProviderConfig config;

    public RemoteRpcImplementation(ActorRef rpcBroker, SchemaContext schemaContext, RemoteRpcProviderConfig config) {
        this.rpcBroker = rpcBroker;
        this.schemaContext = schemaContext;
        this.config = config;
    }

    @Override
    public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(QName rpc,
            YangInstanceIdentifier identifier, CompositeNode input) {
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
                new Timeout(config.getAskDuration()));

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

        return listenableFuture;
    }
}
