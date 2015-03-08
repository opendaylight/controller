package org.opendaylight.controller.md.sal.binding.impl;

import java.util.Collection;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

abstract class LazySerializedDOMRpcResult implements DOMRpcResult {

    private final RpcResult<?> bindingResult;

    LazySerializedDOMRpcResult(RpcResult<?> bindingResult) {
        this.bindingResult = bindingResult;
    }

    static final LazySerializedDOMRpcResult create(RpcResult<?> bindingResult, BindingNormalizedNodeCodecRegistry codec) {
        final Object resultData = bindingResult.getResult();

        if(resultData instanceof DataObject) {
            return new DataResult(bindingResult,codec);


        }
        return new EmptyDataResult(bindingResult);
    }

    RpcResult<?> bidningRpcResult() {
        return bindingResult;
    }

    @Override
    public Collection<RpcError> getErrors() {
        return bindingResult.getErrors();
    }


    private static final class DataResult extends LazySerializedDOMRpcResult {

        private BindingNormalizedNodeCodecRegistry codec;
        private NormalizedNode<?, ?> domData;

        public DataResult(RpcResult<?> bindingResult, BindingNormalizedNodeCodecRegistry codec) {
            super(bindingResult);
            this.codec = codec;
        }

        @Override
        public NormalizedNode<?, ?> getResult() {
            if(domData == null) {
                domData = codec.toNormalizedNodeRpcData((DataContainer) bidningRpcResult().getResult());
            }
            return domData;
        }
    }


    private static final class EmptyDataResult extends LazySerializedDOMRpcResult {

        public EmptyDataResult(RpcResult<?> bindingResult) {
            super(bindingResult);
        }

        @Override
        public NormalizedNode<?, ?> getResult() {
            // FIXME: Should we return something else?
            return null;
        }

    }



}
