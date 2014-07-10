/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nullable;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.sal.connect.api.MessageTransformer;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceCommunicator;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
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


        return Futures.transform(delegateFutureWithPureResult, new Function<RpcResult<NetconfMessage>, RpcResult<CompositeNode>>() {
            @Override
            public RpcResult<CompositeNode> apply(@Nullable final RpcResult<NetconfMessage> input) {
                return transformResult(input, rpc);
            }
        });
    }

    private NetconfMessage transformRequest(final QName rpc, final CompositeNode input) {
        return transformer.toRpcRequest(rpc, input);
    }

    private RpcResult<CompositeNode> transformResult(final RpcResult<NetconfMessage> netconfMessageRpcResult,
                                                                  final QName rpc) {
        if (netconfMessageRpcResult.isSuccessful()) {
            return transformer.toRpcResult(netconfMessageRpcResult.getResult(), rpc);
        } else {
            return RpcResultBuilder.<CompositeNode> failed()
                                      .withRpcErrors(netconfMessageRpcResult.getErrors()).build();
        }
    }

}
