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
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DefaultDOMRpcException;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;

/**
 * Adapts {@link org.opendaylight.mdsal.dom.api.DOMRpcResult} to {@link DOMRpcResult}.
 *
 * @author Thomas Pantelis
 */
public class LegacyDOMRpcResultFutureAdapter extends AbstractDOMRpcResultFutureAdapter<DOMRpcResult, DOMRpcException,
        org.opendaylight.mdsal.dom.api.DOMRpcResult, org.opendaylight.mdsal.dom.api.DOMRpcException> {

    private static final ExceptionMapper<DOMRpcException> LEGACY_DOM_RPC_EX_MAPPER =
            new ExceptionMapper<DOMRpcException>("rpc", DOMRpcException.class) {
        @Override
        protected DOMRpcException newWithCause(String message, Throwable cause) {
            return cause instanceof DOMRpcException ? (DOMRpcException)cause
                : cause instanceof org.opendaylight.mdsal.dom.api.DOMRpcImplementationNotAvailableException
                    ? new DOMRpcImplementationNotAvailableException(cause.getMessage(), cause.getCause())
                        : new DefaultDOMRpcException("RPC failed", cause);
        }
    };

    public LegacyDOMRpcResultFutureAdapter(CheckedFuture<org.opendaylight.mdsal.dom.api.DOMRpcResult,
            org.opendaylight.mdsal.dom.api.DOMRpcException> delegate) {
        super(delegate, LEGACY_DOM_RPC_EX_MAPPER);
    }

    @Override
    protected DOMRpcResult transform(org.opendaylight.mdsal.dom.api.DOMRpcResult fromResult) {
        return new DefaultDOMRpcResult(fromResult.getResult(), fromResult.getErrors());
    }
}
