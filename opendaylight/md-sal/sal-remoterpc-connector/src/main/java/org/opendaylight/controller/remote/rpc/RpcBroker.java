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
import akka.japi.Creator;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Arrays;
import java.util.Collection;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeSerializer;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages.Node;
import org.opendaylight.controller.remote.rpc.messages.ExecuteRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actor to initiate execution of remote RPC on other nodes of the cluster.
 */

public class RpcBroker extends AbstractUntypedActor {

    private static final Logger LOG = LoggerFactory.getLogger(RpcBroker.class);
    private final DOMRpcService rpcService;

    private RpcBroker(final DOMRpcService rpcService) {
        this.rpcService = rpcService;
    }

    public static Props props(final DOMRpcService rpcService) {
        Preconditions.checkNotNull(rpcService, "DOMRpcService can not be null");
        return Props.create(new RpcBrokerCreator(rpcService));
    }

    @Override
    protected void handleReceive(final Object message) throws Exception {
        if (message instanceof ExecuteRpc) {
            executeRpc((ExecuteRpc) message);
        }
    }

    private void executeRpc(final ExecuteRpc msg) {
        LOG.debug("Executing rpc {}", msg.getRpc());
        final NormalizedNode<?, ?> input = RemoteRpcInput.from(msg.getInputNormalizedNode());
        final SchemaPath schemaPath = SchemaPath.create(true, msg.getRpc());
        final ActorRef sender = getSender();
        final ActorRef self = self();

        try {
            final CheckedFuture<DOMRpcResult, DOMRpcException> future = rpcService.invokeRpc(schemaPath, input);

            Futures.addCallback(future, new FutureCallback<DOMRpcResult>() {
                @Override
                public void onSuccess(final DOMRpcResult result) {
                    if (result.getErrors() != null && (!result.getErrors().isEmpty())) {
                        final String message = String.format("Execution of RPC %s failed", msg.getRpc());
                        Collection<RpcError> errors = result.getErrors();
                        if (errors == null || errors.size() == 0) {
                            errors = Arrays.asList(RpcResultBuilder.newError(ErrorType.RPC, null, message));
                        }

                        sender.tell(new akka.actor.Status.Failure(new RpcErrorsException(message, errors)), self);
                    } else {
                        final Node serializedResultNode;
                        if (result.getResult() == null) {
                            serializedResultNode = null;
                        } else {
                            serializedResultNode = NormalizedNodeSerializer.serialize(result.getResult());
                        }

                        LOG.debug("Sending response for execute rpc : {}", msg.getRpc());

                        sender.tell(new RpcResponse(serializedResultNode), self);
                    }
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("executeRpc for {} failed with root cause: {}. For exception details, enable Debug logging.",
                        msg.getRpc(), Throwables.getRootCause(t));
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Detailed exception for execute RPC failure :{}", t);
                    }
                    sender.tell(new akka.actor.Status.Failure(t), self);
                }
            });
        } catch (final Exception e) {
            sender.tell(new akka.actor.Status.Failure(e), sender);
        }
    }

    private static class RpcBrokerCreator implements Creator<RpcBroker> {
        private static final long serialVersionUID = 1L;

        final DOMRpcService rpcService;

        RpcBrokerCreator(final DOMRpcService rpcService) {
            this.rpcService = rpcService;
        }

        @Override
        public RpcBroker create() throws Exception {
            return new RpcBroker(rpcService);
        }
    }
}
