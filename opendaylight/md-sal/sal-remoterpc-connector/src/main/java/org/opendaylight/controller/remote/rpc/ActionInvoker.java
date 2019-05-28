package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.remote.rpc.messages.ActionResponse;
import org.opendaylight.controller.remote.rpc.messages.ExecuteAction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMActionResult;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class ActionInvoker extends AbstractUntypedActor {
    private final DOMActionService actionService;

    private ActionInvoker(final DOMActionService actionService) {
        this.actionService = Preconditions.checkNotNull(actionService);
    }

    public static Props props( final DOMActionService actionService) {
        Preconditions.checkNotNull(actionService, "DOMActionService can not be null");
        return Props.create(ActionInvoker.class, actionService);
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof ExecuteAction) {
            executeAction((ExecuteAction) message );
        }
        else {
            unknownMessage(message);
        }
    }

    private void executeAction(final ExecuteAction msg) {
        LOG.debug("Executing Action {}", msg.getAction());
        final SchemaPath schemaPath = SchemaPath.create(true, msg.getAction());
        final ActorRef sender = getSender();
        final ActorRef self = self();

        final ListenableFuture<? extends DOMActionResult> future;
        try {
            final YangInstanceIdentifier action_path = YangInstanceIdentifier.create(
                    new YangInstanceIdentifier.NodeIdentifier(msg.getAction()));
            future = actionService.invokeAction(schemaPath, new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, action_path), msg.getContainerNode().get());
        } catch (final RuntimeException e) {
            LOG.debug("Failed to invoke Action {}", msg.getAction(), e);
            sender.tell(new akka.actor.Status.Failure(e), sender);
            return;
        }

        Futures.addCallback(future, new FutureCallback<DOMActionResult>() {
            @Override
            public void onSuccess(final DOMActionResult result) {
                if (result == null) {
                    // This shouldn't happen but the FutureCallback annotates the result param with Nullable so
                    // handle null here to avoid FindBugs warning.
                    LOG.debug("Got null DOMActionResult - sending null response for execute Action : {}", msg.getAction());
                    sender.tell(new ActionResponse(null), self);
                    return;
                }

                if (!result.getErrors().isEmpty()) {
                    final String message = String.format("Execution of Action %s failed", msg.getAction());
                    sender.tell(new akka.actor.Status.Failure(new RpcErrorsException(message, result.getErrors())),
                            self);
                } else {
                    LOG.debug("Sending response for execute Action : {}", msg.getAction());
                    sender.tell(new ActionResponse(result.getOutput()), self);
                }
            }

            @Override
            public void onFailure(final Throwable failure) {
                LOG.debug("Failed to execute Action {}", msg.getAction(), failure);
                LOG.error("Failed to execute Action {} due to {}. More details are available on DEBUG level.",
                        msg.getAction(), Throwables.getRootCause(failure).getMessage());
                sender.tell(new akka.actor.Status.Failure(failure), self);
            }
        }, MoreExecutors.directExecutor());
    }
}

