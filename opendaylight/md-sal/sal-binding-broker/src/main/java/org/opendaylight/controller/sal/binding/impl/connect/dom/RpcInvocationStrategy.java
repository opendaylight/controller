/*
 ** Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 **
 ** This program and the accompanying materials are made available under the
 ** terms of the Eclipse Public License v1.0 which accompanies this distribution,
 ** and is available at http://www.eclipse.org/legal/epl-v10.html
 **/

package org.opendaylight.controller.sal.binding.impl.connect.dom;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.Future;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;

/*
 * RPC's can have both input, output, one or the other, or neither.
 *
 * This class handles the permutations and provides two means of invocation:
 * 1. forwardToDomBroker
 * 2.
 *
 * Weak References to the input and output classes are used to allow these classes to
 * be from another OSGi bundle/class loader which may come and go.
 *
 */
public class RpcInvocationStrategy {
    private final Function<RpcResult<CompositeNode>, RpcResult<?>> transformationFunction = new Function<RpcResult<CompositeNode>, RpcResult<?>>() {
        @SuppressWarnings("rawtypes")
        @Override
        public RpcResult<?> apply(final RpcResult<CompositeNode> result) {
            final Object output;
            if (getOutputClass() != null && result.getResult() != null) {
                output = mappingService.dataObjectFromDataDom(getOutputClass().get(), result.getResult());
            } else {
                output = null;
            }

            return RpcResultBuilder.from( (RpcResult)result ).withResult( output ).build();
        }
    };

    private final BindingIndependentMappingService mappingService;
    private final RpcProvisionRegistry biRpcRegistry;
    protected final Method targetMethod;
    protected final QName rpc;

    @SuppressWarnings("rawtypes")
    private final WeakReference<Class> inputClass;

    @SuppressWarnings("rawtypes")
    private final WeakReference<Class> outputClass;

    @SuppressWarnings({ "rawtypes" })
    public RpcInvocationStrategy(final QName rpc,
                                 final Method targetMethod,
                                 final BindingIndependentMappingService mappingService,
                                 final RpcProvisionRegistry biRpcRegistry ) {
        this.mappingService = mappingService;
        this.biRpcRegistry = biRpcRegistry;
        this.targetMethod = targetMethod;
        this.rpc = rpc;

        final Optional<Class<?>> outputClassOption = BindingReflections.resolveRpcOutputClass(targetMethod);
        if (outputClassOption.isPresent()) {
            this.outputClass = new WeakReference(outputClassOption.get());
        } else {
            this.outputClass = null;
        }

        final Optional<Class<? extends DataContainer>> inputClassOption = BindingReflections.resolveRpcInputClass(targetMethod);
        if (inputClassOption.isPresent() ) {
            this.inputClass = new WeakReference(inputClassOption.get());
        } else {
            this.inputClass = null;
        }
    }

    @SuppressWarnings({ "unchecked" })
    public ListenableFuture<RpcResult<?>> forwardToDomBroker(final DataObject input) {

        if(biRpcRegistry == null) {
            return Futures.<RpcResult<?>> immediateFuture(RpcResultBuilder.failed().build());
        }

        CompositeNode inputXml = null;
        if( input != null ) {
            CompositeNode xml = mappingService.toDataDom(input);
            inputXml = ImmutableCompositeNode.create(rpc, ImmutableList.<Node<?>> of(xml));
        } else {
            inputXml = ImmutableCompositeNode.create( rpc, Collections.<Node<?>>emptyList() );
        }

        return Futures.transform(biRpcRegistry.invokeRpc(rpc, inputXml), transformationFunction);
    }

    @SuppressWarnings("unchecked")
    private RpcResult<CompositeNode> uncheckedInvoke(final RpcService rpcService, final CompositeNode domInput) throws Exception {

        Future<RpcResult<?>> futureResult = null;

        if( inputClass != null ){
            DataContainer bindingInput = mappingService.dataObjectFromDataDom(inputClass.get(), domInput);
            futureResult = (Future<RpcResult<?>>) targetMethod.invoke(rpcService, bindingInput);

        } else {
            futureResult = (Future<RpcResult<?>>) targetMethod.invoke(rpcService);
        }

        if (futureResult == null) {
            return RpcResultBuilder.<CompositeNode>failed().build();
        }

        @SuppressWarnings("rawtypes")
        RpcResult bindingResult = futureResult.get();

        final Object resultObj = bindingResult.getResult();
        Object output = null;
        if (resultObj instanceof DataObject) {
            output = mappingService.toDataDom((DataObject)resultObj);
        }
        return RpcResultBuilder.from( bindingResult ).withResult( output ).build();
    }

    public RpcResult<CompositeNode> invokeOn(final RpcService rpcService, final CompositeNode domInput) throws Exception {
        return uncheckedInvoke(rpcService, domInput);
    }

    @SuppressWarnings("rawtypes")
    @VisibleForTesting
    WeakReference<Class> getOutputClass() {
        return outputClass;
    }
}
