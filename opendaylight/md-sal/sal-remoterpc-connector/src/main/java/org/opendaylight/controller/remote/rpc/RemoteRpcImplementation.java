/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.remote.rpc.messages.ExecuteRpc;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * A {@link DOMRpcImplementation} which routes invocation requests to a remote invoker actor.
 *
 * @author Robert Varga
 */
final class RemoteRpcImplementation implements DOMRpcImplementation {
    // 0 for local, 1 for binding, 2 for remote
    private static final long COST = 2;

    private final ActorRef remoteInvoker;
    private final Timeout askDuration;

    RemoteRpcImplementation(final ActorRef remoteInvoker, final RemoteRpcProviderConfig config) {
        this.remoteInvoker = Preconditions.checkNotNull(remoteInvoker);
        this.askDuration = config.getAskDuration();
    }

    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final DOMRpcIdentifier rpc,
            final NormalizedNode<?, ?> input) {
        final RemoteDOMRpcFuture ret = RemoteDOMRpcFuture.create(rpc.getType().getLastComponent());
        ret.completeWith(Patterns.ask(remoteInvoker, ExecuteRpc.from(rpc, input), askDuration));
        return ret;
    }

    @Override
    public long invocationCost() {
        return COST;
    }
}
