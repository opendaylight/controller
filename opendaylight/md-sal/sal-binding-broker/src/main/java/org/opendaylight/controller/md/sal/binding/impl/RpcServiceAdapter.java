/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.broker.spi.rpc.RpcRoutingStrategy;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

class RpcServiceAdapter implements InvocationHandler {

    private final ImmutableMap<Method, RpcInvocationStrategy> rpcNames;
    private final Class<? extends RpcService> type;
    private final BindingToNormalizedNodeCodec codec;
    private final DOMRpcService delegate;
    private final RpcService proxy;

    RpcServiceAdapter(final Class<? extends RpcService> type, final BindingToNormalizedNodeCodec codec,
            final DOMRpcService domService) {
        this.type = Preconditions.checkNotNull(type);
        this.codec = Preconditions.checkNotNull(codec);
        this.delegate = Preconditions.checkNotNull(domService);
        final ImmutableMap.Builder<Method, RpcInvocationStrategy> rpcBuilder = ImmutableMap.builder();
        for (final Entry<Method, RpcDefinition> rpc : codec.getRpcMethodToSchema(type).entrySet()) {
            rpcBuilder.put(rpc.getKey(), createStrategy(rpc.getKey(), rpc.getValue()));
        }
        rpcNames = rpcBuilder.build();
        proxy = (RpcService) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {type}, this);
    }

    private ListenableFuture<RpcResult<?>> invoke0(final SchemaPath schemaPath, final NormalizedNode<?, ?> input) {
        final CheckedFuture<DOMRpcResult, DOMRpcException> result = delegate.invokeRpc(schemaPath, input);
        if(result instanceof LazyDOMRpcResultFuture) {
            return ((LazyDOMRpcResultFuture) result).getBindingFuture();
        }

        return transformFuture(schemaPath, result, codec.getCodecFactory());
    }

    private RpcInvocationStrategy createStrategy(final Method method, final RpcDefinition schema) {
        final RpcRoutingStrategy strategy = RpcRoutingStrategy.from(schema);
        if (strategy.isContextBasedRouted()) {
            return new RoutedStrategy(schema.getPath(), method, strategy.getLeaf());
        }
        return new NonRoutedStrategy(schema.getPath());
    }

    RpcService getProxy() {
        return proxy;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

        final RpcInvocationStrategy rpc = rpcNames.get(method);
        if (rpc != null) {
            if (method.getParameterTypes().length == 0) {
                return rpc.invokeEmpty();
            }
            if (args.length != 1) {
                throw new IllegalArgumentException("Input must be provided.");
            }
            return rpc.invoke((DataObject) args[0]);
        }

        if (isObjectMethod(method)) {
            return callObjectMethod(proxy, method, args);
        }
        throw new UnsupportedOperationException("Method " + method.toString() + "is unsupported.");
    }

    private static boolean isObjectMethod(final Method m) {
        switch (m.getName()) {
            case "toString":
                return (m.getReturnType().equals(String.class) && m.getParameterTypes().length == 0);
            case "hashCode":
                return (m.getReturnType().equals(int.class) && m.getParameterTypes().length == 0);
            case "equals":
                return (m.getReturnType().equals(boolean.class) && m.getParameterTypes().length == 1 && m
                        .getParameterTypes()[0] == Object.class);
            default:
                return false;
        }
    }

    private Object callObjectMethod(final Object self, final Method m, final Object[] args) {
        switch (m.getName()) {
            case "toString":
                return type.getName() + "$Adapter{delegate=" + delegate.toString() + "}";
            case "hashCode":
                return System.identityHashCode(self);
            case "equals":
                return (self == args[0]);
            default:
                return null;
        }
    }

    private static ListenableFuture<RpcResult<?>> transformFuture(final SchemaPath rpc,
            final ListenableFuture<DOMRpcResult> domFuture, final BindingNormalizedNodeCodecRegistry codec) {
        return Futures.transform(domFuture, new Function<DOMRpcResult, RpcResult<?>>() {
            @Override
            public RpcResult<?> apply(final DOMRpcResult input) {
                final NormalizedNode<?, ?> domData = input.getResult();
                final DataObject bindingResult;
                if (domData != null) {
                    final SchemaPath rpcOutput = rpc.createChild(QName.create(rpc.getLastComponent(), "output"));
                    bindingResult = codec.fromNormalizedNodeRpcData(rpcOutput, (ContainerNode) domData);
                } else {
                    bindingResult = null;
                }
                return RpcResult.class.cast(RpcResultBuilder.success(bindingResult).build());
            }
        });
    }

    private abstract class RpcInvocationStrategy {

        private final SchemaPath rpcName;

        protected RpcInvocationStrategy(final SchemaPath path) {
            rpcName = path;
        }

        final ListenableFuture<RpcResult<?>> invoke(final DataObject input) {
            return invoke0(rpcName, serialize(input));
        }

        abstract NormalizedNode<?, ?> serialize(DataObject input);

        final ListenableFuture<RpcResult<?>> invokeEmpty() {
            return invoke0(rpcName, null);
        }

        final SchemaPath getRpcName() {
            return rpcName;
        }

    }

    private final class NonRoutedStrategy extends RpcInvocationStrategy {

        protected NonRoutedStrategy(final SchemaPath path) {
            super(path);
        }

        @Override
        NormalizedNode<?, ?> serialize(final DataObject input) {
            return LazySerializedContainerNode.create(getRpcName(), input, codec.getCodecRegistry());
        }

    }

    private final class RoutedStrategy extends RpcInvocationStrategy {

        private final ContextReferenceExtractor refExtractor;
        private final NodeIdentifier contextName;

        protected RoutedStrategy(final SchemaPath path, final Method rpcMethod, final QName leafName) {
            super(path);
            final Class<? extends DataContainer> inputType = BindingReflections.resolveRpcInputClass(rpcMethod).get();
            refExtractor = ContextReferenceExtractor.from(inputType);
            this.contextName = new NodeIdentifier(leafName);
        }

        @Override
        NormalizedNode<?, ?> serialize(final DataObject input) {
            final InstanceIdentifier<?> bindingII = refExtractor.extract(input);
            if (bindingII != null) {
                final YangInstanceIdentifier yangII = codec.toYangInstanceIdentifierCached(bindingII);
                final LeafNode<?> contextRef = ImmutableNodes.leafNode(contextName, yangII);
                return LazySerializedContainerNode.withContextRef(getRpcName(), input, contextRef,
                        codec.getCodecRegistry());
            }
            return LazySerializedContainerNode.create(getRpcName(), input, codec.getCodecRegistry());
        }

    }
}
