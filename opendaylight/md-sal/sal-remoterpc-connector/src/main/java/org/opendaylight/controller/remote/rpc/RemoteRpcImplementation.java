package org.opendaylight.controller.remote.rpc;

import static akka.pattern.Patterns.ask;
import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeSerializer;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.remote.rpc.messages.InvokeRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;

public class RemoteRpcImplementation implements DOMRpcImplementation {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteRpcImplementation.class);
    private final ActorRef rpcBroker;
    private final RemoteRpcProviderConfig config;

    public RemoteRpcImplementation(final ActorRef rpcBroker, final RemoteRpcProviderConfig config) {
        this.rpcBroker = rpcBroker;
        this.config = config;
    }

    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final DOMRpcIdentifier rpc, final NormalizedNode<?, ?> input) {
        final InvokeRpc rpcMsg = new InvokeRpc(rpc.getType().getLastComponent(), rpc.getContextReference(), input);

        final SettableFuture<DOMRpcResult> settableFuture = SettableFuture.create();

        final ListenableFuture<DOMRpcResult> listenableFuture =
                JdkFutureAdapters.listenInPoolThread(settableFuture);

        final scala.concurrent.Future<Object> future = ask(rpcBroker, rpcMsg, config.getAskDuration());

        final OnComplete<Object> onComplete = new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable failure, final Object reply) throws Throwable {
                if(failure != null) {
                    LOG.error("InvokeRpc failed", failure);

                    final String message = String.format("Execution of RPC %s failed",  rpcMsg.getRpc());
                    Collection<RpcError> errors = ((RpcErrorsException)failure).getRpcErrors();
                    if(errors == null || errors.size() == 0) {
                        errors = Arrays.asList(RpcResultBuilder.newError(ErrorType.RPC, null, message));
                    }
                    final DOMRpcResult rpcResult = new DefaultDOMRpcResult(errors);

                    settableFuture.set(rpcResult);
                    return;
                }

                final RpcResponse rpcReply = (RpcResponse)reply;
                final NormalizedNode<?, ?> result =
                        NormalizedNodeSerializer.deSerialize(rpcReply.getResultNormalizedNode());
                settableFuture.set(new DefaultDOMRpcResult(result));
            }
        };


        future.onComplete(onComplete, ExecutionContext.Implicits$.MODULE$.global());
        // FIXME find non blocking way for implementation
        try {
            return Futures.immediateCheckedFuture(listenableFuture.get());
        }
        catch (InterruptedException | ExecutionException e) {
            LOG.debug("Unexpected remote RPC exception.", e);
            return Futures.immediateFailedCheckedFuture((DOMRpcException) new DOMRpcImplementationNotAvailableException(e, "Unexpected remote RPC exception"));
        }
    }
}
