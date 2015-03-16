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
import akka.dispatch.OnComplete;
import akka.japi.Creator;
import akka.japi.Pair;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;

import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.remote.rpc.messages.ExecuteRpc;
import org.opendaylight.controller.remote.rpc.messages.InvokeRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.controller.remote.rpc.messages.UpdateSchemaContext;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.utils.LatestEntryRoutingLogic;
import org.opendaylight.controller.remote.rpc.utils.RoutingLogic;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.xml.codec.XmlUtils;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import static akka.pattern.Patterns.ask;

/**
 * Actor to initiate execution of remote RPC on other nodes of the cluster.
 */

public class RpcBroker extends AbstractUntypedActor {

    private static final Logger LOG = LoggerFactory.getLogger(RpcBroker.class);
    private final Broker.ProviderSession brokerSession;
    private final ActorRef rpcRegistry;
    private SchemaContext schemaContext;
    private final RemoteRpcProviderConfig config;

    private RpcBroker(Broker.ProviderSession brokerSession, ActorRef rpcRegistry,
            SchemaContext schemaContext) {
        this.brokerSession = brokerSession;
        this.rpcRegistry = rpcRegistry;
        this.schemaContext = schemaContext;
        config = new RemoteRpcProviderConfig(getContext().system().settings().config());
    }

    public static Props props(Broker.ProviderSession brokerSession, ActorRef rpcRegistry,
            SchemaContext schemaContext) {
        return Props.create(new RpcBrokerCreator(brokerSession, rpcRegistry, schemaContext));
    }

    @Override
    protected void handleReceive(Object message) throws Exception {
        if(message instanceof InvokeRpc) {
            invokeRemoteRpc((InvokeRpc) message);
        } else if(message instanceof ExecuteRpc) {
            executeRpc((ExecuteRpc) message);
        } else if(message instanceof UpdateSchemaContext) {
            updateSchemaContext((UpdateSchemaContext) message);
        }
    }

    private void updateSchemaContext(UpdateSchemaContext message) {
        this.schemaContext = message.getSchemaContext();
    }

    private void invokeRemoteRpc(final InvokeRpc msg) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Looking up the remote actor for rpc {}", msg.getRpc());
        }
        RpcRouter.RouteIdentifier<?,?,?> routeId = new RouteIdentifierImpl(
                null, msg.getRpc(), msg.getIdentifier());
        RpcRegistry.Messages.FindRouters findMsg = new RpcRegistry.Messages.FindRouters(routeId);

        scala.concurrent.Future<Object> future = ask(rpcRegistry, findMsg, config.getAskDuration());

        final ActorRef sender = getSender();
        final ActorRef self = self();

        OnComplete<Object> onComplete = new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object reply) throws Throwable {
                if(failure != null) {
                    LOG.error("FindRouters failed", failure);
                    sender.tell(new akka.actor.Status.Failure(failure), self);
                    return;
                }

                RpcRegistry.Messages.FindRoutersReply findReply =
                                                (RpcRegistry.Messages.FindRoutersReply)reply;

                List<Pair<ActorRef, Long>> actorRefList = findReply.getRouterWithUpdateTime();

                if(actorRefList == null || actorRefList.isEmpty()) {
                    String message = String.format(
                            "No remote implementation found for rpc %s",  msg.getRpc());
                    sender.tell(new akka.actor.Status.Failure(new RpcErrorsException(
                            message, Arrays.asList(RpcResultBuilder.newError(ErrorType.RPC,
                                    "operation-not-supported", message)))), self);
                    return;
                }

                finishInvokeRpc(actorRefList, msg, sender, self);
            }
        };

        future.onComplete(onComplete, getContext().dispatcher());
    }

    protected void finishInvokeRpc(final List<Pair<ActorRef, Long>> actorRefList,
            final InvokeRpc msg, final ActorRef sender, final ActorRef self) {

        RoutingLogic logic = new LatestEntryRoutingLogic(actorRefList);

        ExecuteRpc executeMsg = new ExecuteRpc(XmlUtils.inputCompositeNodeToXml(msg.getInput(),
                schemaContext), msg.getRpc());

        scala.concurrent.Future<Object> future = ask(logic.select(), executeMsg, config.getAskDuration());

        OnComplete<Object> onComplete = new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object reply) throws Throwable {
                if(failure != null) {
                    LOG.error("ExecuteRpc failed", failure);
                    sender.tell(new akka.actor.Status.Failure(failure), self);
                    return;
                }

                sender.tell(reply, self);
            }
        };

        future.onComplete(onComplete, getContext().dispatcher());
    }

    private void executeRpc(final ExecuteRpc msg) {
//        if(LOG.isDebugEnabled()) {
//            LOG.debug("Executing rpc {}", msg.getRpc());
//        }
//        Future<RpcResult<CompositeNode>> future = brokerSession.rpc(msg.getRpc(),
//                XmlUtils.inputXmlToCompositeNode(msg.getRpc(), msg.getInputCompositeNode(),
//                        schemaContext));
//
//        ListenableFuture<RpcResult<CompositeNode>> listenableFuture =
//                JdkFutureAdapters.listenInPoolThread(future);
//
//        final ActorRef sender = getSender();
//        final ActorRef self = self();
//
//        Futures.addCallback(listenableFuture, new FutureCallback<RpcResult<CompositeNode>>() {
//            @Override
//            public void onSuccess(RpcResult<CompositeNode> result) {
//                if(result.isSuccessful()) {
//                    sender.tell(new RpcResponse(XmlUtils.outputCompositeNodeToXml(result.getResult(),
//                            schemaContext)), self);
//                } else {
//                    String message = String.format("Execution of RPC %s failed",  msg.getRpc());
//                    Collection<RpcError> errors = result.getErrors();
//                    if(errors == null || errors.size() == 0) {
//                        errors = Arrays.asList(RpcResultBuilder.newError(ErrorType.RPC,
//                                null, message));
//                    }
//
//                    sender.tell(new akka.actor.Status.Failure(new RpcErrorsException(
//                            message, errors)), self);
//                }
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//                LOG.error("executeRpc for {} failed: {}", msg.getRpc(), t);
//                sender.tell(new akka.actor.Status.Failure(t), self);
//            }
//        });
    }

    private static class RpcBrokerCreator implements Creator<RpcBroker> {
        private static final long serialVersionUID = 1L;

        final Broker.ProviderSession brokerSession;
        final ActorRef rpcRegistry;
        final SchemaContext schemaContext;

        RpcBrokerCreator(ProviderSession brokerSession, ActorRef rpcRegistry,
                SchemaContext schemaContext) {
            this.brokerSession = brokerSession;
            this.rpcRegistry = rpcRegistry;
            this.schemaContext = schemaContext;
        }

        @Override
        public RpcBroker create() throws Exception {
            return new RpcBroker(brokerSession, rpcRegistry, schemaContext);
        }
    }
}
