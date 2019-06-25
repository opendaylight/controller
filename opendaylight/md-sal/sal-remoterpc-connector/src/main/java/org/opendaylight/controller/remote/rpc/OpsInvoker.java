/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Status.Failure;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.remote.rpc.messages.ExecuteAction;
import org.opendaylight.controller.remote.rpc.messages.ExecuteRpc;
import org.opendaylight.controller.remote.rpc.messages.OpsResponse;
import org.opendaylight.mdsal.dom.api.DOMActionResult;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Actor receiving invocation requests from remote nodes, routing them to
 * {@link DOMRpcService#invokeRpc(SchemaPath, NormalizedNode)}.
 */
final class OpsInvoker extends AbstractUntypedActor {
    private final DOMRpcService rpcService;
    private final DOMActionService actionService;

    private OpsInvoker(final DOMRpcService rpcService, final DOMActionService actionService) {
        this.rpcService = requireNonNull(rpcService);
        this.actionService = requireNonNull(actionService);
    }

    public static Props props(final DOMRpcService rpcService, final DOMActionService actionService) {
        return Props.create(OpsInvoker.class,
            requireNonNull(rpcService, "DOMRpcService can not be null"),
            requireNonNull(actionService, "DOMActionService can not be null"));
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof ExecuteRpc) {
            LOG.debug("Handling ExecuteOps Message");
            execute((ExecuteRpc) message);
        } else if (message instanceof ExecuteAction) {
            execute((ExecuteAction) message);
        } else {
            unknownMessage(message);
        }
    }

    private void execute(final ExecuteRpc msg) {
        LOG.debug("Executing RPC {}", msg.getType());
        final ActorRef sender = getSender();

        final ListenableFuture<DOMRpcResult> future;
        try {
            future = rpcService.invokeRpc(msg.getType(), msg.getInput());
        } catch (final RuntimeException e) {
            LOG.debug("Failed to invoke RPC {}", msg.getType(), e);
            sender.tell(new Failure(e), self());
            return;
        }

        Futures.addCallback(future, new AbstractCallback<DOMRpcResult>(getSender(), msg.getType()) {
            @Override
            Collection<? extends RpcError> extractErrors(final DOMRpcResult result) {
                return result.getErrors();
            }

            @Override
            NormalizedNode<?, ?> extractOutput(final DOMRpcResult result) {
                return result.getResult();
            }
        }, MoreExecutors.directExecutor());
    }

    private void execute(final ExecuteAction msg) {
        LOG.debug("Executing Action {}", msg.getType());

        final ActorRef sender = getSender();

        final ListenableFuture<? extends DOMActionResult> future;
        try {
            future = actionService.invokeAction(msg.getType(), msg.getPath(), msg.getInput());
        } catch (final RuntimeException e) {
            LOG.debug("Failed to invoke action {}", msg.getType(), e);
            sender.tell(new Failure(e), self());
            return;
        }

        Futures.addCallback(future, new AbstractCallback<DOMActionResult>(getSender(), msg.getType()) {
            @Override
            Collection<? extends RpcError> extractErrors(final DOMActionResult result) {
                return result.getErrors();
            }

            @Override
            NormalizedNode<?, ?> extractOutput(final DOMActionResult result) {
                return result.getOutput().orElse(null);
            }
        }, MoreExecutors.directExecutor());
    }

    private abstract class AbstractCallback<T> implements FutureCallback<T> {
        private final ActorRef replyTo;
        private final SchemaPath type;

        AbstractCallback(final ActorRef replyTo, final SchemaPath type) {
            this.replyTo = requireNonNull(replyTo);
            this.type = requireNonNull(type);
        }

        @Override
        public final void onSuccess(final T result) {
            final Object response;
            if (result != null) {
                final Collection<? extends RpcError> errors = extractErrors(result);
                if (errors.isEmpty()) {
                    response = new OpsResponse(extractOutput(result));
                } else {
                    response = new Failure(new RpcErrorsException(String.format("Execution of action %s failed", type),
                        errors));
                }
            } else {
                // This shouldn't happen but the FutureCallback annotates the result param with Nullable so
                // handle null here to avoid FindBugs warning.
                LOG.debug("Got null DOMActionResult - sending null response for execute Action : {}", type);
                response = new OpsResponse((Optional<ContainerNode>)null);
            }

            LOG.debug("Sending response for execution of {} : {}", type, response);
            replyTo.tell(response, self());
        }

        @Override
        public final void onFailure(final Throwable failure) {
            LOG.debug("Failed to execute operation {}", type, failure);
            LOG.error("Failed to execute operation {} due to {}. More details are available on DEBUG level.", type,
                Throwables.getRootCause(failure).getMessage());
            replyTo.tell(new Failure(failure), self());
        }

        abstract @NonNull Collection<? extends RpcError> extractErrors(@NonNull T result);

        abstract @Nullable NormalizedNode<?, ?> extractOutput(@NonNull T result);
    }
}
