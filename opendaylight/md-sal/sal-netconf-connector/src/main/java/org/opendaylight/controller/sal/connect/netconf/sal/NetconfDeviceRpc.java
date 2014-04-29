/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.connect.api.MessageTransformer;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceCommunicator;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Invokes RPC by sending netconf message via listener. Also transforms result from NetconfMessage to CompositeNode.
 */
public final class NetconfDeviceRpc implements RpcImplementation {
    private final RemoteDeviceCommunicator<NetconfMessage> listener;
    private final MessageTransformer<NetconfMessage> transformer;

    public NetconfDeviceRpc(final RemoteDeviceCommunicator<NetconfMessage> listener, final MessageTransformer<NetconfMessage> transformer) {
        this.listener = listener;
        this.transformer = transformer;
    }

    @Override
    public Set<QName> getSupportedRpcs() {
        // TODO is this correct ?
        return Collections.emptySet();
    }

    @Override
    public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(final QName rpc, final CompositeNode input) {
        final NetconfMessage message = transformRequest(rpc, input);
        final ListenableFuture<RpcResult<NetconfMessage>> delegateFutureWithPureResult = listener.sendRequest(
                message, rpc);

        return new TransformingListenableFuture<NetconfMessage, CompositeNode>(delegateFutureWithPureResult) {

            @Override
            public RpcResult<CompositeNode> get() throws InterruptedException, ExecutionException {
                final RpcResult<NetconfMessage> netconfMessageRpcResult = delegateFutureWithPureResult.get();
                return transformResult(netconfMessageRpcResult, rpc);
            }

            @Override
            public RpcResult<CompositeNode> get(final long timeout, final TimeUnit unit) throws InterruptedException,
                    ExecutionException, TimeoutException {
                final RpcResult<NetconfMessage> netconfMessageRpcResult = delegateFutureWithPureResult.get(timeout,
                        unit);
                return transformResult(netconfMessageRpcResult, rpc);
            }
        };
    }

    private NetconfMessage transformRequest(final QName rpc, final CompositeNode input) {
        return transformer.toRpcRequest(rpc, input);
    }

    private RpcResult<CompositeNode> transformResult(final RpcResult<NetconfMessage> netconfMessageRpcResult,
                                                                  final QName rpc) {
        if (netconfMessageRpcResult.isSuccessful()) {
            return transformer.toRpcResult(netconfMessageRpcResult.getResult(), rpc);
        } else {
            return Rpcs.getRpcResult(false, netconfMessageRpcResult.getErrors());
        }
    }

    /**
     *
     * Transforms result of delegate Future
     *
     * @param <F> from type
     * @param <T> to type
     */
    public static abstract class TransformingListenableFuture<F, T> implements ListenableFuture<RpcResult<T>> {
        private final ListenableFuture<RpcResult<F>> delegateFutureWithPureResult;

        public TransformingListenableFuture(final ListenableFuture<RpcResult<F>> delegateFutureWithPureResult) {
            this.delegateFutureWithPureResult = delegateFutureWithPureResult;
        }

        @Override
        public void addListener(final Runnable listener, final Executor executor) {
            delegateFutureWithPureResult.addListener(listener, executor);
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            return delegateFutureWithPureResult.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return delegateFutureWithPureResult.isCancelled();
        }

        @Override
        public boolean isDone() {
            return delegateFutureWithPureResult.isDone();
        }

        @Override
        public abstract RpcResult<T> get() throws InterruptedException, ExecutionException;

        @Override
        public abstract RpcResult<T> get(final long timeout, final TimeUnit unit) throws InterruptedException,
                ExecutionException, TimeoutException;
    }
}
