/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FluentFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DefaultDOMRpcException;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;

/**
 * Adapts a {@link org.opendaylight.mdsal.dom.api.DOMRpcResult} CheckedFuture to a {@link DOMRpcResult} CheckedFuture.
 *
 * @author Thomas Pantelis
 */
public class LegacyDOMRpcResultFutureAdapter extends AbstractDOMRpcResultFutureAdapter<DOMRpcResult,
        org.opendaylight.mdsal.dom.api.DOMRpcResult, FluentFuture<org.opendaylight.mdsal.dom.api.DOMRpcResult>,
        DOMRpcException> implements CheckedFuture<DOMRpcResult, DOMRpcException> {

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

    public LegacyDOMRpcResultFutureAdapter(FluentFuture<org.opendaylight.mdsal.dom.api.DOMRpcResult> delegate) {
        super(delegate, LEGACY_DOM_RPC_EX_MAPPER);
    }

    @Override
    @SuppressFBWarnings("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE")
    public DOMRpcResult checkedGet() throws DOMRpcException {
        try {
            return get();
        } catch (InterruptedException | ExecutionException e) {
            throw LEGACY_DOM_RPC_EX_MAPPER.apply(e);
        }
    }

    @Override
    @SuppressFBWarnings("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE")
    public DOMRpcResult checkedGet(final long timeout, final TimeUnit unit) throws TimeoutException, DOMRpcException {
        try {
            return get(timeout, unit);
        } catch (InterruptedException | ExecutionException e) {
            throw LEGACY_DOM_RPC_EX_MAPPER.apply(e);
        }
    }

    @Override
    protected DOMRpcResult transform(org.opendaylight.mdsal.dom.api.DOMRpcResult fromResult) {
        return new DefaultDOMRpcResult(fromResult.getResult(), fromResult.getErrors());
    }
}
