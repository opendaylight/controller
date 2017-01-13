/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.remote.rpc.messages.ExecuteRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Actor receiving invocation requests from remote nodes, routing them to
 * {@link DOMRpcService#invokeRpc(SchemaPath, NormalizedNode)}.
 */
final class RpcInvoker extends AbstractUntypedActor {
    private final DOMRpcService rpcService;

    private RpcInvoker(final DOMRpcService rpcService) {
        this.rpcService = Preconditions.checkNotNull(rpcService);
    }

    public static Props props(final DOMRpcService rpcService) {
        Preconditions.checkNotNull(rpcService, "DOMRpcService can not be null");
        return Props.create(RpcInvoker.class, rpcService);
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof ExecuteRpc) {
            executeRpc((ExecuteRpc) message);
        } else {
            unknownMessage(message);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void executeRpc(final ExecuteRpc msg) {
        LOG.debug("Executing rpc {}", msg.getRpc());
        final SchemaPath schemaPath = SchemaPath.create(true, msg.getRpc());
        final ActorRef sender = getSender();
        final ActorRef self = self();

        final CheckedFuture<DOMRpcResult, DOMRpcException> future;
        try {
            future = rpcService.invokeRpc(schemaPath, msg.getInputNormalizedNode());
        } catch (final RuntimeException e) {
            LOG.debug("Failed to invoke RPC {}", msg.getRpc(), e);
            sender.tell(new akka.actor.Status.Failure(e), sender);
            return;
        }

        Futures.addCallback(future, new FutureCallback<DOMRpcResult>() {
            @Override
            public void onSuccess(final DOMRpcResult result) {
                if (result == null) {
                    // This shouldn't happen but the FutureCallback annotates the result param with Nullable so
                    // handle null here to avoid FindBugs warning.
                    LOG.debug("Got null DOMRpcResult - sending null response for execute rpc : {}", msg.getRpc());
                    sender.tell(new RpcResponse(null), self);
                    return;
                }

                if (!result.getErrors().isEmpty()) {
                    final String message = String.format("Execution of RPC %s failed", msg.getRpc());
                    sender.tell(new akka.actor.Status.Failure(new RpcErrorsException(message, result.getErrors())),
                        self);
                } else {
                    LOG.debug("Sending response for execute rpc : {}", msg.getRpc());
                    sender.tell(new RpcResponse(result.getResult()), self);
                }
            }

            @Override
            public void onFailure(final Throwable failure) {
                LOG.debug("Failed to execute RPC {}", msg.getRpc(), failure);
                LOG.error("Failed to execute RPC {} due to {}. More details are available on DEBUG level.",
                    msg.getRpc(), Throwables.getRootCause(failure));
                sender.tell(new akka.actor.Status.Failure(failure), self);
            }
        });
    }
}
