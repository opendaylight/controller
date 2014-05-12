package org.opendaylight.controller.sal.binding.impl.connect.dom;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class RpcInvocationStrategy {

    private final BindingIndependentMappingService mappingService;
    private final RpcProvisionRegistry biRpcRegistry;
    protected final Method targetMethod;
    protected final QName rpc;

    @SuppressWarnings("rawtypes")
    private final WeakReference<Class> inputClass;

    @SuppressWarnings("rawtypes")
    private final WeakReference<Class> outputClass;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public RpcInvocationStrategy(final QName rpc,
                                 final Method targetMethod,
                                 final Class<?> outputClass,
                                 final Class<? extends DataContainer> inputClass,
                                 final BindingIndependentMappingService mappingService,
                                 final RpcProvisionRegistry biRpcRegistry ) {
        this.targetMethod = targetMethod;
        this.rpc = rpc;
        this.outputClass = new WeakReference(outputClass);
        this.inputClass = new WeakReference(inputClass);
        this.mappingService = mappingService;
        this.biRpcRegistry = biRpcRegistry;
    }

    public ListenableFuture<RpcResult<?>> forwardToDomBroker(final DataObject input) {
        if(biRpcRegistry == null) {
            return Futures.<RpcResult<?>> immediateFuture(Rpcs.getRpcResult(false));
        }

        CompositeNode xml = mappingService.toDataDom(input);
        CompositeNode wrappedXml = ImmutableCompositeNode.create(rpc, ImmutableList.<Node<?>> of(xml));

        Function<RpcResult<CompositeNode>, RpcResult<?>> transformationFunction =
                                       new Function<RpcResult<CompositeNode>, RpcResult<?>>() {
            @Override
            public RpcResult<?> apply(RpcResult<CompositeNode> result) {

                Object output = null;

                if( getOutputClass() != null ) {
                    if (result.getResult() != null) {
                        output =
                                mappingService.dataObjectFromDataDom(getOutputClass().get(),
                                                                    result.getResult());
                    }
                }

                return Rpcs.getRpcResult(result.isSuccessful(), output, result.getErrors());
            }
        };

        return Futures.transform(biRpcRegistry.invokeRpc(rpc, wrappedXml), transformationFunction);
    }

    @SuppressWarnings("unchecked")
    public RpcResult<CompositeNode> uncheckedInvoke(final RpcService rpcService, final CompositeNode domInput) throws Exception {
        Future<RpcResult<?>> futureResult = null;
        if( inputClass != null ){
            DataContainer bindingInput = mappingService.dataObjectFromDataDom(inputClass.get(), domInput);
            futureResult = (Future<RpcResult<?>>) targetMethod.invoke(rpcService, bindingInput);
        } else {
            futureResult = (Future<RpcResult<?>>) targetMethod.invoke(rpcService);
        }

        if (futureResult == null) {
            return Rpcs.getRpcResult(false);
        }

        RpcResult<?> bindingResult = futureResult.get();

        Collection<RpcError> errors = bindingResult.getErrors();
        if( errors == null ) {
            errors = Collections.<RpcError>emptySet();
        }

        final Object resultObj = bindingResult.getResult();
        CompositeNode output = null;
        if (resultObj instanceof DataObject) {
            output = mappingService.toDataDom((DataObject)resultObj);
        }
        return Rpcs.getRpcResult( bindingResult.isSuccessful(), output, errors);
    }

    public RpcResult<CompositeNode> invokeOn(final RpcService rpcService, final CompositeNode domInput) throws Exception {
        return uncheckedInvoke(rpcService, domInput);
    }

    @SuppressWarnings("rawtypes")
    public WeakReference<Class> getOutputClass() {
        return outputClass;
    }
}
