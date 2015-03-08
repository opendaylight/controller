/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.lang.reflect.Method;
import org.opendaylight.controller.md.sal.binding.impl.RpcServiceAdapter.InvocationDelegate;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class BindingDOMRpcServiceAdapter implements RpcConsumerRegistry, InvocationDelegate {

    private final LoadingCache<Class<? extends RpcService>, RpcServiceAdapter> proxies = CacheBuilder.newBuilder()
            .weakKeys()
            .build(new CacheLoader<Class<? extends RpcService>, RpcServiceAdapter>() {

                @Override
                public RpcServiceAdapter load(Class<? extends RpcService> key) throws Exception {
                    return createProxy(key);
                }

            });

    private final DOMRpcService domService;
    private final BindingToNormalizedNodeCodec codec;

    public BindingDOMRpcServiceAdapter(DOMRpcService domService, BindingToNormalizedNodeCodec codec) {
        super();
        this.domService = domService;
        this.codec = codec;
    }

    @Override
    public <T extends RpcService> T getRpcService(Class<T> rpcService) {
        Preconditions.checkArgument(rpcService != null, "Rpc Service needs to be specied.");
        @SuppressWarnings("unchecked")
        T proxy = (T) proxies.getUnchecked(rpcService).getProxy();
        return proxy;
    }

    @Override
    public ListenableFuture<RpcResult<?>> invoke(SchemaPath rpc, DataObject input) {
        CheckedFuture<DOMRpcResult, DOMRpcException> domFuture = domService.invokeRpc(rpc, serialize(rpc,input));
        return transformFuture(rpc,domFuture,codec.getCodecFactory());
    }

    private RpcServiceAdapter createProxy(Class<? extends RpcService> key) {
        Preconditions.checkArgument(BindingReflections.isBindingClass(key));
        Preconditions.checkArgument(key.isInterface(), "Supplied RPC service type must be interface.");
        ImmutableMap<Method, SchemaPath> rpcNames = codec.getRpcMethodToSchemaPath(key);
        return new RpcServiceAdapter(key, rpcNames, this);
    }

    private NormalizedNode<?, ?> serialize(SchemaPath rpc,DataObject input) {
        if(input == null) {
            return null;
        }
        QName rpcInputIdentifier = QName.create(rpc.getLastComponent(),"input");
        return new LazySerializedContainerNode(rpcInputIdentifier, input, codec.getCodecFactory());
    }

    private static ListenableFuture<RpcResult<?>> transformFuture(final SchemaPath rpc,final ListenableFuture<DOMRpcResult> domFuture, final BindingNormalizedNodeCodecRegistry codec) {
        return Futures.transform(domFuture, new Function<DOMRpcResult, RpcResult<?>>() {
            @Override
            public RpcResult<?> apply(DOMRpcResult input) {
                if(input instanceof LazySerializedDOMRpcResult) {
                    return ((LazySerializedDOMRpcResult) input).bidningRpcResult();
                }
                NormalizedNode<?, ?> domData = input.getResult();
                final DataObject bindingResult;
                if(domData != null) {
                    SchemaPath rpcOutput = rpc.createChild(QName.create(rpc.getLastComponent(),"output"));
                    bindingResult = codec.fromNormalizedNodeRpcData(rpcOutput, (ContainerNode) domData);
                } else {
                    bindingResult = null;
                }
                return RpcResult.class.cast(RpcResultBuilder.success(bindingResult).build());
            }
        });
    }

}
