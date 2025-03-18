/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import java.util.concurrent.CompletionStage;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.mdsal.dom.api.DOMRpcException;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.yangtools.yang.common.QName;

final class RemoteDOMRpcFuture extends AbstractRemoteFuture<QName, DOMRpcResult, DOMRpcException> {
    RemoteDOMRpcFuture(final @NonNull QName type, final @NonNull CompletionStage<Object> requestFuture) {
        super(type, requestFuture);
    }

    @Override
    DOMRpcResult processReply(final Object reply) {
        return reply instanceof RpcResponse response ? new DefaultDOMRpcResult(response.getOutput()) : null;
    }

    @Override
    Class<DOMRpcException> exceptionClass() {
        return DOMRpcException.class;
    }

    @Override
    DOMRpcException wrapCause(final Throwable cause) {
        return new RemoteDOMRpcException("Exception during invoking RPC", cause);
    }
}
