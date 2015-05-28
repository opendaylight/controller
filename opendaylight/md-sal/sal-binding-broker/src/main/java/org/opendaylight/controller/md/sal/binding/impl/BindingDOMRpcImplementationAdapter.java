/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.binding.util.RpcServiceInvoker;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class BindingDOMRpcImplementationAdapter implements DOMRpcImplementation {

    private static final Cache<Class<?>, RpcServiceInvoker> SERVICE_INVOKERS = CacheBuilder.newBuilder().weakKeys().build();

    private final BindingNormalizedNodeCodecRegistry codec;
    private final RpcServiceInvoker invoker;
    private final RpcService delegate;
    private final QName inputQname;

    public <T extends RpcService> BindingDOMRpcImplementationAdapter(final BindingNormalizedNodeCodecRegistry codec, final Class<T> type, final Map<SchemaPath, Method> localNameToMethod, final T delegate) {
        try {
            this.invoker = SERVICE_INVOKERS.get(type, new Callable<RpcServiceInvoker>() {
                @Override
                public RpcServiceInvoker call() {
                    final Map<QName, Method> map = new HashMap<>();
                    for (Entry<SchemaPath, Method> e : localNameToMethod.entrySet()) {
                        map.put(e.getKey().getLastComponent(), e.getValue());
                    }

                    return RpcServiceInvoker.from(map);
                }
            });
        } catch (ExecutionException e) {
            throw new IllegalArgumentException("Failed to create invokers for type " + type, e);
        }

        this.codec = Preconditions.checkNotNull(codec);
        this.delegate = Preconditions.checkNotNull(delegate);
        inputQname = QName.cachedReference(QName.create(BindingReflections.getQNameModule(type), "input"));
    }

    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final DOMRpcIdentifier rpc, final NormalizedNode<?, ?> input) {
        final SchemaPath schemaPath = rpc.getType();
        final DataObject bindingInput = input != null ? deserilialize(rpc.getType(),input) : null;
        final ListenableFuture<RpcResult<?>> bindingResult = invoke(schemaPath,bindingInput);
        return transformResult(schemaPath,bindingResult);
    }

    private DataObject deserilialize(final SchemaPath rpcPath, final NormalizedNode<?, ?> input) {
        if (input instanceof LazySerializedContainerNode) {
            return ((LazySerializedContainerNode) input).bindingData();
        }
        final SchemaPath inputSchemaPath = rpcPath.createChild(inputQname);
        return codec.fromNormalizedNodeRpcData(inputSchemaPath, (ContainerNode) input);
    }

    private ListenableFuture<RpcResult<?>> invoke(final SchemaPath schemaPath, final DataObject input) {
        return JdkFutureAdapters.listenInPoolThread(invoker.invokeRpc(delegate, schemaPath.getLastComponent(), input));
    }

    private CheckedFuture<DOMRpcResult, DOMRpcException> transformResult(final SchemaPath schemaPath,
            final ListenableFuture<RpcResult<?>> bindingResult) {
        return LazyDOMRpcResultFuture.create(codec, bindingResult);
    }

}
