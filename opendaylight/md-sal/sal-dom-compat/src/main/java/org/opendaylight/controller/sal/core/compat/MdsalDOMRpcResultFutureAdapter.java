/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.mdsal.dom.api.DOMRpcException;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DefaultDOMRpcException;
import org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;

/**
 * Adapts a {@link org.opendaylight.controller.md.sal.dom.api.DOMRpcResult} CheckedFuture to a
 * {@link DOMRpcResult} CheckedFuture.
 *
 * @author Thomas Pantelis
 */
public class MdsalDOMRpcResultFutureAdapter extends AbstractDOMRpcResultFutureAdapter<
        DOMRpcResult, org.opendaylight.controller.md.sal.dom.api.DOMRpcResult,
        CheckedFuture<org.opendaylight.controller.md.sal.dom.api.DOMRpcResult,
            org.opendaylight.controller.md.sal.dom.api.DOMRpcException>, DOMRpcException> {
    private static final ExceptionMapper<DOMRpcException> MDSAL_DOM_RPC_EX_MAPPER =
            new ExceptionMapper<DOMRpcException>("rpc", DOMRpcException.class) {
        @Override
        protected DOMRpcException newWithCause(String message, Throwable cause) {
            return cause instanceof DOMRpcException ? (DOMRpcException) cause
                    : new DefaultDOMRpcException("RPC failed", cause);
        }
    };

    public MdsalDOMRpcResultFutureAdapter(CheckedFuture<org.opendaylight.controller.md.sal.dom.api.DOMRpcResult,
            org.opendaylight.controller.md.sal.dom.api.DOMRpcException> delegate) {
        super(delegate, MDSAL_DOM_RPC_EX_MAPPER);
    }

    @Override
    protected DOMRpcResult transform(org.opendaylight.controller.md.sal.dom.api.DOMRpcResult fromResult) {
        return new DefaultDOMRpcResult(fromResult.getResult(), fromResult.getErrors());
    }
}
