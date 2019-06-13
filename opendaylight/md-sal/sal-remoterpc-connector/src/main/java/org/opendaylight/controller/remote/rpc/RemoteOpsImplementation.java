/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.remote.rpc.messages.ExecuteOps;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMActionImplementation;
import org.opendaylight.mdsal.dom.api.DOMActionResult;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.mdsal.dom.api.DOMRpcImplementation;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DOMRpcImplementation} and {@link DOMActionImplementation} which routes invocation requests
 * to a remote invoker actor.
 *
 * @author Robert Varga
 */
final class RemoteOpsImplementation implements DOMRpcImplementation, DOMActionImplementation {
    // 0 for local, 1 for binding, 2 for remote
    private static final long COST = 2;

    private final ActorRef remoteInvoker;
    private final Timeout askDuration;
    private static final Logger LOG = LoggerFactory.getLogger(RemoteOpsImplementation.class);

    RemoteOpsImplementation(final ActorRef remoteInvoker, final RemoteOpsProviderConfig config) {
        this.remoteInvoker = requireNonNull(remoteInvoker);
        this.askDuration = config.getAskDuration();
    }

    @Override
    public ListenableFuture<DOMRpcResult> invokeRpc(final DOMRpcIdentifier rpc,
                                                    final NormalizedNode<?, ?> input) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("invoking rpc with Identifier {}",rpc);
        }
        final RemoteDOMRpcFuture ret = RemoteDOMRpcFuture.create(rpc.getType().getLastComponent());
        ret.completeWith(Patterns.ask(remoteInvoker, ExecuteOps.from(rpc.getType().getLastComponent(),
                new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL,
                        YangInstanceIdentifier.create(new YangInstanceIdentifier.NodeIdentifier(
                                rpc.getType().getLastComponent()))),
                input, true), askDuration));
        return ret;
    }


    /**
     * Routes action request to a remote invoker, which will execute the action and return with result.
     */
    @Override
    public ListenableFuture<DOMActionResult> invokeAction(final SchemaPath type, final DOMDataTreeIdentifier path,
                                                          final ContainerNode input) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("invoking action with path {}" ,path);
        }
        final RemoteDOMActionFuture ret = RemoteDOMActionFuture.create(type.getLastComponent());
        ret.completeWith(Patterns.ask(remoteInvoker, ExecuteOps.from(type.getLastComponent(), path,
               input, false), askDuration));
        return ret;
    }

    @Override
    public long invocationCost() {
        return COST;
    }
}
