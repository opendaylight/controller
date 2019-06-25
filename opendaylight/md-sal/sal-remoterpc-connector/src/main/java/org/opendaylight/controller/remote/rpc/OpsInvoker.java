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
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Optional;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.remote.rpc.messages.ExecuteOps;
import org.opendaylight.controller.remote.rpc.messages.OpsResponse;
import org.opendaylight.mdsal.dom.api.DOMActionResult;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
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

    private OpsInvoker(final DOMRpcService rpcService, DOMActionService actionService) {
        this.rpcService = requireNonNull(rpcService);
        this.actionService = requireNonNull(actionService);
    }

    public static Props props(final DOMRpcService rpcService, final DOMActionService actionService) {
        requireNonNull(rpcService, "DOMRpcService can not be null");
        requireNonNull(actionService, "DOMActionService can not be null");
        return Props.create(OpsInvoker.class, rpcService, actionService);
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof ExecuteOps) {
            ExecuteOps executeOps = (ExecuteOps) message;
            LOG.debug("Handling ExecuteOps Message");
            executeOps(executeOps);
        }
        else {
            unknownMessage(message);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void executeOps(final ExecuteOps msg) {
        LOG.debug("Executing Operation {}", msg.getName());
        final SchemaPath schemaPath = SchemaPath.create(true, msg.getName());
        final ActorRef sender = getSender();
        final ActorRef self = self();
        if (msg.getIsRpcMessage()) {
            final ListenableFuture<DOMRpcResult> future;
            try {
                future = rpcService.invokeRpc(schemaPath, msg.getInputNormalizedNode());
            } catch (final RuntimeException e) {
                LOG.debug("Failed to invoke RPC {}", msg.getName(), e);
                sender.tell(new akka.actor.Status.Failure(e), sender);
                return;
            }

            Futures.addCallback(future, new FutureCallback<DOMRpcResult>() {
                    @Override
                    public void onSuccess(final DOMRpcResult result) {
                        if (result == null) {
                            // This shouldn't happen but the FutureCallback annotates the result param with Nullable so
                            // handle null here to avoid FindBugs warning.
                            LOG.debug("Got null DOMRpcResult - sending null response for execute rpc : {}",
                                    msg.getName());
                            sender.tell(new OpsResponse((NormalizedNode<?, ?>) null), self);
                            return;
                        }
                        if (!result.getErrors().isEmpty()) {
                            final String message = String.format("Execution of RPC %s failed", msg.getName());
                            sender.tell(new akka.actor.Status.Failure(new RpcErrorsException(message,
                                            result.getErrors())), self);
                        } else {
                            LOG.debug("Sending response for execute rpc : {}", msg.getName());
                            sender.tell(new OpsResponse(result.getResult()), self);
                        }
                    }

                    @Override
                    public void onFailure(final Throwable failure) {
                        LOG.debug("Failed to execute Action {}", msg.getName(), failure);
                        LOG.error("Failed to execute Action {} due to {}. More details are available on DEBUG level.",
                                msg.getName(), Throwables.getRootCause(failure).getMessage());
                        sender.tell(new akka.actor.Status.Failure(failure), self);
                    }
                }, MoreExecutors.directExecutor());
        }
        else {
            final ListenableFuture<? extends DOMActionResult> future;
            try {
                future = actionService.invokeAction(schemaPath, msg.getPath(),
                        (ContainerNode)msg.getInputNormalizedNode());
            } catch (final RuntimeException e) {
                LOG.debug("Failed to invoke action {}", msg.getName(), e);
                sender.tell(new akka.actor.Status.Failure(e), sender);
                return;
            }

            Futures.addCallback(future, new FutureCallback<DOMActionResult>() {
                @Override
                public void onSuccess(final DOMActionResult result) {
                    if (result == null) {
                        // This shouldn't happen but the FutureCallback annotates the result param with Nullable so
                        // handle null here to avoid FindBugs warning.
                        LOG.debug("Got null DOMActionResult - sending null response for execute Action : {}",
                                msg.getName());
                        sender.tell(new OpsResponse((Optional<ContainerNode>)null), self);
                        return;
                    }

                    if (!result.getErrors().isEmpty()) {
                        final String message = String.format("Execution of action %s failed", msg.getName());
                        sender.tell(new akka.actor.Status.Failure(new RpcErrorsException(message, result.getErrors())),
                                self);
                    } else {
                        LOG.debug("Sending response for execute action : {}", msg.getName());
                        sender.tell(new OpsResponse(result.getOutput()), self);
                    }
                }

                @Override
                public void onFailure(final Throwable failure) {
                    LOG.debug("Failed to execute action {}", msg.getName(), failure);
                    LOG.error("Failed to execute action {} due to {}. More details are available on DEBUG level.",
                            msg.getName(), Throwables.getRootCause(failure).getMessage());
                    sender.tell(new akka.actor.Status.Failure(failure), self);
                }
            }, MoreExecutors.directExecutor());
        }

    }
}
