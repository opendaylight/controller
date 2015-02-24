/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.sal.connect.api.MessageTransformer;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceCommunicator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Invokes RPC by sending netconf message via listener. Also transforms result from NetconfMessage to CompositeNode.
 */
public final class NetconfDeviceRpc implements DOMRpcService {

    private static final Function<RpcDefinition, DOMRpcIdentifier> RPC_TO_RPC_IDENTIFIER = new Function<RpcDefinition, DOMRpcIdentifier>() {
        @Override
        public DOMRpcIdentifier apply(final RpcDefinition input) {
            // TODO add support for routed rpcs ... is it necessary in this case ?
            return DOMRpcIdentifier.create(input.getPath());
        }
    };

    private final RemoteDeviceCommunicator<NetconfMessage> listener;
    private final MessageTransformer<NetconfMessage> transformer;
    private final Collection<DOMRpcIdentifier> availableRpcs;

    public NetconfDeviceRpc(final SchemaContext schemaContext, final RemoteDeviceCommunicator<NetconfMessage> listener, final MessageTransformer<NetconfMessage> transformer) {
        this.listener = listener;
        this.transformer = transformer;

        availableRpcs = Collections2.transform(schemaContext.getOperations(), RPC_TO_RPC_IDENTIFIER);
    }

    @Nonnull
    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull final SchemaPath type, @Nullable final NormalizedNode<?, ?> input) {
        Preconditions.checkArgument(input instanceof ContainerNode, "Epc payload has to be a %s, was %s", ContainerNode.class, input);

        final NetconfMessage message = transformer.toRpcRequest(type, (ContainerNode) input);
        final ListenableFuture<RpcResult<NetconfMessage>> delegateFutureWithPureResult = listener.sendRequest(message, type.getLastComponent());

        final ListenableFuture<DOMRpcResult> transformed = Futures.transform(delegateFutureWithPureResult, new Function<RpcResult<NetconfMessage>, DOMRpcResult>() {
            @Override
            public DOMRpcResult apply(final RpcResult<NetconfMessage> input) {
                if (input.isSuccessful()) {
                    return transformer.toRpcResult(input.getResult(), type);
                } else {
                    // TODO check whether the listener sets errors properly
                    return new DefaultDOMRpcResult(input.getErrors());
                }
            }
        });

        return Futures.makeChecked(transformed, new Function<Exception, DOMRpcException>() {
            @Nullable
            @Override
            public DOMRpcException apply(@Nullable final Exception e) {
                // FIXME what other possible exceptions are there ?
                return new DOMRpcImplementationNotAvailableException(e, "Unable to invoke rpc %s", type);
            }
        });
    }

    @Nonnull
    @Override
    public <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(@Nonnull final T listener) {

        listener.onRpcAvailable(availableRpcs);

        return new ListenerRegistration<T>() {
            @Override
            public void close() {
                // NOOP, no rpcs appear and disappear in this implementation
            }

            @Override
            public T getInstance() {
                return listener;
            }
        };
    }
}
