package org.opendaylight.controller.remote.rpc;

import static akka.pattern.Patterns.ask;

import akka.actor.ActorRef;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.remote.rpc.messages.InvokeRpc;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class RemoteRpcImplementation implements DOMRpcImplementation {
    private final ActorRef rpcBroker;
    private final RemoteRpcProviderConfig config;

    public RemoteRpcImplementation(final ActorRef rpcBroker, final RemoteRpcProviderConfig config) {
        this.rpcBroker = rpcBroker;
        this.config = config;
    }

    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final DOMRpcIdentifier rpc, final NormalizedNode<?, ?> input) {
        final InvokeRpc rpcMsg = new InvokeRpc(rpc.getType().getLastComponent(), rpc.getContextReference(), input);
        final scala.concurrent.Future<Object> future = ask(rpcBroker, rpcMsg, config.getAskDuration());
        return RemoteDOMRpcFuture.from(future);
    }
}
