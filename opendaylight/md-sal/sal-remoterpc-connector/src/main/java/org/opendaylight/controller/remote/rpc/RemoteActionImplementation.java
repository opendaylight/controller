package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.remote.rpc.messages.ExecuteAction;
import org.opendaylight.mdsal.dom.api.DOMActionImplementation;
import org.opendaylight.mdsal.dom.api.DOMActionResult;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class RemoteActionImplementation implements DOMActionImplementation {
    // 0 for local, 1 for binding, 2 for remote
    private static final long COST = 2;

    private final ActorRef remoteInvoker;
    private final Timeout askDuration;

    RemoteActionImplementation(final ActorRef remoteInvoker, final RemoteRpcProviderConfig config) {
        this.remoteInvoker = requireNonNull(remoteInvoker);
        this.askDuration = config.getAskDuration();
    }

    @Override
    public ListenableFuture<DOMActionResult> invokeAction(final SchemaPath type, final DOMDataTreeIdentifier path,
                                                          final ContainerNode input) {

        final RemoteDOMActionFuture ret = RemoteDOMActionFuture.create(type.getLastComponent());
        ret.completeWith(Patterns.ask(remoteInvoker, ExecuteAction.from(type.getLastComponent(), Optional.of((input))), askDuration));
        return ret;
    }

    @Override
    public long invocationCost() {
        return COST;
    }
}