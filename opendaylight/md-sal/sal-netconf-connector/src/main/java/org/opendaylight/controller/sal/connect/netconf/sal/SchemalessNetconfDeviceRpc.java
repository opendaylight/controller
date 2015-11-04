/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.Collections;
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
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableAnyXmlNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.w3c.dom.Document;

/**
 * Invokes RPC by sending netconf message via listener. Also transforms result from NetconfMessage to CompositeNode.
 */
public final class SchemalessNetconfDeviceRpc implements DOMRpcService {

    private final RemoteDeviceCommunicator<NetconfMessage> listener;

    public SchemalessNetconfDeviceRpc(final RemoteDeviceCommunicator<NetconfMessage> listener) {
        this.listener = listener;
    }

    @Nonnull
    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull final SchemaPath type, @Nullable final NormalizedNode<?, ?> input) {
        // TODO WRAP rpc in "rpc" XML element

        final ListenableFuture<RpcResult<NetconfMessage>> rpcResultListenableFuture = listener
            .sendRequest(new NetconfMessage(((Document) ((AnyXmlNode) input).getValue().getNode())), null);

        final ListenableFuture<DOMRpcResult> transformed = Futures.transform(rpcResultListenableFuture, new Function<RpcResult<NetconfMessage>, DOMRpcResult>() {
            @Override
            public DOMRpcResult apply(final RpcResult<NetconfMessage> input) {
                if (input.isSuccessful()) {
                    // implement
//                    new ImmutableAnyXmlNodeBuilder().withValue(input.getResult());
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
        throw new UnsupportedOperationException("Not available for netconf 1.0");
    }
}
