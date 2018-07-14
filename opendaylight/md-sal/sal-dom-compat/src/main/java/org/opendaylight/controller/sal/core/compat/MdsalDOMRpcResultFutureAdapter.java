/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;

/**
 * Adapts {@link DOMRpcResult} to {@link org.opendaylight.mdsal.dom.api.DOMRpcResult}.
 *
 * @author Thomas Pantelis
 */
public class MdsalDOMRpcResultFutureAdapter extends AbstractDOMRpcResultFutureAdapter<
        org.opendaylight.mdsal.dom.api.DOMRpcResult, org.opendaylight.mdsal.dom.api.DOMRpcException,
        DOMRpcResult, DOMRpcException> {
    private static final ExceptionMapper<org.opendaylight.mdsal.dom.api.DOMRpcException> MDSAL_DOM_RPC_EX_MAPPER =
            new ExceptionMapper<org.opendaylight.mdsal.dom.api.DOMRpcException>(
                    "rpc", org.opendaylight.mdsal.dom.api.DOMRpcException.class) {
        @Override
        protected org.opendaylight.mdsal.dom.api.DOMRpcException newWithCause(String message, Throwable cause) {
            return cause instanceof org.opendaylight.mdsal.dom.api.DOMRpcException
                    ? (org.opendaylight.mdsal.dom.api.DOMRpcException)cause
                            : new org.opendaylight.mdsal.dom.api.DefaultDOMRpcException("RPC failed", cause);
        }
    };

    public MdsalDOMRpcResultFutureAdapter(CheckedFuture<DOMRpcResult, DOMRpcException> delegate) {
        super(delegate, MDSAL_DOM_RPC_EX_MAPPER);
    }

    @Override
    protected org.opendaylight.mdsal.dom.api.DOMRpcResult transform(DOMRpcResult fromResult) {
        return new org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult(fromResult.getResult(), fromResult.getErrors());
    }
}
