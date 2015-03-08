/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
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

    LazySerializedDOMRpcResult(final RpcResult<?> bindingResult) {
        this.bindingResult = bindingResult;
    }

    static final LazySerializedDOMRpcResult create(final RpcResult<?> bindingResult, final BindingNormalizedNodeCodecRegistry codec) {
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

        private final BindingNormalizedNodeCodecRegistry codec;
        private NormalizedNode<?, ?> domData;

        public DataResult(final RpcResult<?> bindingResult, final BindingNormalizedNodeCodecRegistry codec) {
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

        public EmptyDataResult(final RpcResult<?> bindingResult) {
            super(bindingResult);
        }

        @Override
        public NormalizedNode<?, ?> getResult() {
            // FIXME: Should we return something else?
            return null;
        }

    }



}
