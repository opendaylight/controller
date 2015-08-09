/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import static akka.pattern.Patterns.ask;

import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.japi.Pair;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.List;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.remote.rpc.messages.ExecuteRpc;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.FindRoutersReply;
import org.opendaylight.controller.remote.rpc.utils.LatestEntryRoutingLogic;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

public class RemoteRpcImplementation implements DOMRpcImplementation {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteRpcImplementation.class);

    private final ActorRef rpcRegistry;
    private final RemoteRpcProviderConfig config;

    public RemoteRpcImplementation(final ActorRef rpcRegistry, final RemoteRpcProviderConfig config) {
        this.config = config;
        this.rpcRegistry = rpcRegistry;
    }

    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final DOMRpcIdentifier rpc,
            final NormalizedNode<?, ?> input) {
        if (input instanceof RemoteRpcInput) {
            LOG.warn("Rpc {} was removed during execution or there is loop present. Failing received rpc.", rpc);
            return Futures
                    .<DOMRpcResult, DOMRpcException>immediateFailedCheckedFuture(new DOMRpcImplementationNotAvailableException(
                            "Rpc implementation for {} was removed during processing.", rpc));
        }
        final RemoteDOMRpcFuture frontEndFuture = RemoteDOMRpcFuture.create(rpc.getType().getLastComponent());
        findRouteAsync(rpc).onComplete(new OnComplete<FindRoutersReply>() {

            @Override
            public void onComplete(final Throwable error, final FindRoutersReply routes) throws Throwable {
                if (error != null) {
                    frontEndFuture.failNow(error);
                } else {
                    final List<Pair<ActorRef, Long>> routePairs = routes.getRouterWithUpdateTime();
                    if (routePairs == null || routePairs.isEmpty()) {
                        frontEndFuture.failNow(new DOMRpcImplementationNotAvailableException(
                                "No local or remote implementation available for rpc %s", rpc.getType(), error));
                    } else {
                        final ActorRef remoteImplRef = new LatestEntryRoutingLogic(routePairs).select();
                        final Object executeRpcMessage = ExecuteRpc.from(rpc, input);
                        LOG.debug("Found remote actor {} for rpc {} - sending {}", remoteImplRef, rpc.getType(), executeRpcMessage);
                        frontEndFuture.completeWith(ask(remoteImplRef, executeRpcMessage, config.getAskDuration()));
                    }
                }
            }
        }, ExecutionContext.Implicits$.MODULE$.global());
        return frontEndFuture;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Future<FindRoutersReply> findRouteAsync(final DOMRpcIdentifier rpc) {
        // FIXME: Refactor routeId and message to use DOMRpcIdentifier directly.
        final RpcRouter.RouteIdentifier<?, ?, ?> routeId =
                new RouteIdentifierImpl(null, rpc.getType().getLastComponent(), rpc.getContextReference());
        final RpcRegistry.Messages.FindRouters findMsg = new RpcRegistry.Messages.FindRouters(routeId);
        return (Future) ask(rpcRegistry, findMsg, config.getAskDuration());
    }
}
