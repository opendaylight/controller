/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.mdsal.dom.api.DOMRpcException;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RemoteDOMRpcFuture extends AbstractRemoteFuture<DOMRpcResult> {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteDOMRpcFuture.class);

    RemoteDOMRpcFuture(final @NonNull SchemaPath type) {
        super(type);
    }

    @Override
    ExecutionException mapException(final ExecutionException ex) {
        final Throwable cause = ex.getCause();
        if (cause instanceof DOMRpcException) {
            return ex;
        }
        return new ExecutionException(ex.getMessage(),
                new RemoteDOMRpcException("Exception during invoking RPC", ex.getCause()));
    }

    @Override
    AbstractFutureUpdater newFutureUpdater() {
        return new AbstractFutureUpdater() {
            @Override
            boolean onComplete(final SchemaPath type, final Object reply) {
                if (reply instanceof RpcResponse) {
                    final RpcResponse rpcReply = (RpcResponse) reply;
                    final NormalizedNode<?, ?> result = rpcReply.getOutput();

                    LOG.debug("Received response for rpc {}: result is {}", type, result);

                    RemoteDOMRpcFuture.this.set(new DefaultDOMRpcResult(result));

                    LOG.debug("Future {} for rpc {} successfully completed", RemoteDOMRpcFuture.this, type);
                    return true;
                }

                return false;
            }
        };
    }
}
