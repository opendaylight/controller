/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import akka.dispatch.OnComplete;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.mdsal.dom.api.DOMRpcException;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

final class RemoteDOMRpcFuture extends AbstractFuture<DOMRpcResult> {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteDOMRpcFuture.class);

    private final QName rpcName;

    private RemoteDOMRpcFuture(final QName rpcName) {
        this.rpcName = Preconditions.checkNotNull(rpcName, "rpcName");
    }

    public static RemoteDOMRpcFuture create(final QName rpcName) {
        return new RemoteDOMRpcFuture(rpcName);
    }

    protected void failNow(final Throwable error) {
        LOG.debug("Failing future {} for rpc {}", this, rpcName, error);
        setException(error);
    }

    protected void completeWith(final Future<Object> future) {
        future.onComplete(new FutureUpdater(), ExecutionContext.Implicits$.MODULE$.global());
    }

    @Override
    public DOMRpcResult get() throws InterruptedException, ExecutionException {
        try {
            return super.get();
        } catch (ExecutionException e) {
            throw mapException(e);
        }
    }

    @Override
    public DOMRpcResult get(final long timeout, @Nonnull final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return super.get(timeout, unit);
        } catch (final ExecutionException e) {
            throw mapException(e);
        }
    }

    private static ExecutionException mapException(final ExecutionException ex) {
        final Throwable cause = ex.getCause();
        if (cause instanceof DOMRpcException) {
            return ex;
        }
        return new ExecutionException(ex.getMessage(),
                new RemoteDOMRpcException("Exception during invoking RPC", ex.getCause()));
    }

    private final class FutureUpdater extends OnComplete<Object> {

        @Override
        public void onComplete(final Throwable error, final Object reply) {
            if (error != null) {
                RemoteDOMRpcFuture.this.failNow(error);
            } else if (reply instanceof RpcResponse) {
                final RpcResponse rpcReply = (RpcResponse) reply;
                final NormalizedNode<?, ?> result = rpcReply.getResultNormalizedNode();

                LOG.debug("Received response for rpc {}: result is {}", rpcName, result);

                RemoteDOMRpcFuture.this.set(new DefaultDOMRpcResult(result));

                LOG.debug("Future {} for rpc {} successfully completed", RemoteDOMRpcFuture.this, rpcName);
            } else {
                RemoteDOMRpcFuture.this.failNow(new IllegalStateException("Incorrect reply type " + reply
                        + "from Akka"));
            }
        }
    }
}
