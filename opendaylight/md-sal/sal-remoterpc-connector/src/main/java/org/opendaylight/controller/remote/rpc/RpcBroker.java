/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import static akka.pattern.Patterns.ask;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.OnComplete;
import akka.japi.Creator;
import akka.japi.Pair;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeSerializer;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages.Node;
import org.opendaylight.controller.remote.rpc.messages.ExecuteRpc;
import org.opendaylight.controller.remote.rpc.messages.InvokeRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.utils.LatestEntryRoutingLogic;
import org.opendaylight.controller.remote.rpc.utils.RoutingLogic;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
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
    private final ActorRef rpcRegistry;
    private final RemoteRpcProviderConfig config;
    private final DOMRpcService rpcService;

    private RpcBroker(final DOMRpcService rpcService, final ActorRef rpcRegistry) {
        this.rpcService = rpcService;
        this.rpcRegistry = rpcRegistry;
        config = new RemoteRpcProviderConfig(getContext().system().settings().config());
    }

    public static Props props(final DOMRpcService rpcService, final ActorRef rpcRegistry) {
        Preconditions.checkNotNull(rpcRegistry, "ActorRef can not be null!");
        Preconditions.checkNotNull(rpcService, "DOMRpcService can not be null");
        return Props.create(new RpcBrokerCreator(rpcService, rpcRegistry));
    }

    @Override
    protected void handleReceive(final Object message) throws Exception {
        if(message instanceof InvokeRpc) {
            invokeRemoteRpc((InvokeRpc) message);
        } else if(message instanceof ExecuteRpc) {
            executeRpc((ExecuteRpc) message);
        }
    }

    private void invokeRemoteRpc(final InvokeRpc msg) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Looking up the remote actor for rpc {}", msg.getRpc());
        }
        final RpcRouter.RouteIdentifier<?,?,?> routeId = new RouteIdentifierImpl(
                null, msg.getRpc(), msg.getIdentifier());
        final RpcRegistry.Messages.FindRouters findMsg = new RpcRegistry.Messages.FindRouters(routeId);

        final scala.concurrent.Future<Object> future = ask(rpcRegistry, findMsg, config.getAskDuration());

        final ActorRef sender = getSender();
        final ActorRef self = self();

        final OnComplete<Object> onComplete = new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable failure, final Object reply) throws Throwable {
                if(failure != null) {
                    LOG.error("FindRouters failed", failure);
                    sender.tell(new akka.actor.Status.Failure(failure), self);
                    return;
                }

                final RpcRegistry.Messages.FindRoutersReply findReply =
                                                (RpcRegistry.Messages.FindRoutersReply)reply;

                final List<Pair<ActorRef, Long>> actorRefList = findReply.getRouterWithUpdateTime();

                if(actorRefList == null || actorRefList.isEmpty()) {
                    sender.tell(new akka.actor.Status.Failure(new DOMRpcImplementationNotAvailableException(
                            "No remote implementation available for rpc %s", msg.getRpc())), self);
                    return;
                }
                finishInvokeRpc(actorRefList, msg, sender, self);
            }
        };

        future.onComplete(onComplete, getContext().dispatcher());
    }

    protected void finishInvokeRpc(final List<Pair<ActorRef, Long>> actorRefList,
            final InvokeRpc msg, final ActorRef sender, final ActorRef self) {

        final RoutingLogic logic = new LatestEntryRoutingLogic(actorRefList);

        final Node serializedNode = NormalizedNodeSerializer.serialize(msg.getInput());
        final ExecuteRpc executeMsg = new ExecuteRpc(serializedNode, msg.getRpc());

        final scala.concurrent.Future<Object> future = ask(logic.select(), executeMsg, config.getAskDuration());

        final OnComplete<Object> onComplete = new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable failure, final Object reply) throws Throwable {
                if(failure != null) {
                    LOG.error("ExecuteRpc failed", failure);
                    sender.tell(new akka.actor.Status.Failure(failure), self);
                    return;
                }

                LOG.debug("Execute Rpc response received for rpc : {}, responding to sender : {}", msg.getRpc(), sender);

                sender.tell(reply, self);
            }
        };

        future.onComplete(onComplete, getContext().dispatcher());
    }

    private void executeRpc(final ExecuteRpc msg) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Executing rpc {}", msg.getRpc());
        }
        final NormalizedNode<?, ?> input = NormalizedNodeSerializer.deSerialize(msg.getInputNormalizedNode());
        final SchemaPath schemaPath = SchemaPath.create(true, msg.getRpc());

        final CheckedFuture<DOMRpcResult, DOMRpcException> future = rpcService.invokeRpc(schemaPath, input);

        final ListenableFuture<DOMRpcResult> listenableFuture =
                JdkFutureAdapters.listenInPoolThread(future);

        final ActorRef sender = getSender();
        final ActorRef self = self();

        Futures.addCallback(listenableFuture, new FutureCallback<DOMRpcResult>() {
            @Override
            public void onSuccess(final DOMRpcResult result) {
                if (result.getErrors() != null && ( ! result.getErrors().isEmpty())) {
                    final String message = String.format("Execution of RPC %s failed",  msg.getRpc());
                    Collection<RpcError> errors = result.getErrors();
                    if(errors == null || errors.size() == 0) {
                        errors = Arrays.asList(RpcResultBuilder.newError(ErrorType.RPC,
                                null, message));
                    }

                    sender.tell(new akka.actor.Status.Failure(new RpcErrorsException(
                            message, errors)), self);
                } else {
                    final Node serializedResultNode;
                    if(result.getResult() == null){
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
                LOG.error("executeRpc for {} failed: {}", msg.getRpc(), t);
                sender.tell(new akka.actor.Status.Failure(t), self);
            }
        });
    }

    private static class RpcBrokerCreator implements Creator<RpcBroker> {
        private static final long serialVersionUID = 1L;

        final DOMRpcService rpcService;
        final ActorRef rpcRegistry;

        RpcBrokerCreator(final DOMRpcService rpcService, final ActorRef rpcRegistry) {
            this.rpcService = rpcService;
            this.rpcRegistry = rpcRegistry;
        }

        @Override
        public RpcBroker create() throws Exception {
            return new RpcBroker(rpcService, rpcRegistry);
        }
    }
}
