/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.remote.rpc.messages.ExecuteRpc;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.mdsal.dom.api.DOMRpcImplementation;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * A {@link DOMRpcImplementation} which routes invocation requests to a remote invoker actor.
 *
 * @author Robert Varga
 */
final class RemoteRpcImplementation extends AbstractRemoteImplementation<ExecuteRpc> implements DOMRpcImplementation {
    RemoteRpcImplementation(final ActorRef remoteInvoker, final RemoteOpsProviderConfig config) {
        super(remoteInvoker, config);
    }

    @Override
    public ListenableFuture<DOMRpcResult> invokeRpc(final DOMRpcIdentifier rpc,
            final NormalizedNode input) {
        return new RemoteDOMRpcFuture(rpc.getType(), ask(ExecuteRpc.from(rpc, input)));
    }

    @Override
    public long invocationCost() {
        return COST;
    }
}
