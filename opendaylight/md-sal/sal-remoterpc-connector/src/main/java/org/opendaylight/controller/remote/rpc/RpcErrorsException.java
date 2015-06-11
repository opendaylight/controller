/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * An Exception for transferring RpcErrors.
 *
 * @author Thomas Pantelis
 */
public class RpcErrorsException extends DOMRpcException {

    private static final long serialVersionUID = 1L;

    private static class RpcErrorData implements Serializable {
        private static final long serialVersionUID = 1L;

        final ErrorSeverity severity;
        final ErrorType errorType;
        final String tag;
        final String applicationTag;
        final String message;
        final String info;
        final Throwable cause;

        RpcErrorData(final ErrorSeverity severity, final ErrorType errorType, final String tag,
                final String applicationTag, final String message, final String info, final Throwable cause) {
            this.severity = severity;
            this.errorType = errorType;
            this.tag = tag;
            this.applicationTag = applicationTag;
            this.message = message;
            this.info = info;
            this.cause = cause;
        }
    }

    private final List<RpcErrorData> rpcErrorDataList = new ArrayList<>();

    public RpcErrorsException(final String message, final Iterable<RpcError> rpcErrors) {
        super(message);

        for(final RpcError rpcError: rpcErrors) {
            rpcErrorDataList.add(new RpcErrorData(rpcError.getSeverity(), rpcError.getErrorType(),
                    rpcError.getTag(), rpcError.getApplicationTag(), rpcError.getMessage(),
                    rpcError.getInfo(), rpcError.getCause()));
        }
    }

    public Collection<RpcError> getRpcErrors() {
        final Collection<RpcError> rpcErrors = new ArrayList<>();
        for(final RpcErrorData ed: rpcErrorDataList) {
            final RpcError rpcError = ed.severity == ErrorSeverity.ERROR ?
                    RpcResultBuilder.newError(ed.errorType, ed.tag, ed.message, ed.applicationTag,
                            ed.info, ed.cause) :
                    RpcResultBuilder.newWarning(ed.errorType, ed.tag, ed.message, ed.applicationTag,
                            ed.info, ed.cause);
            rpcErrors.add(rpcError);
        }

        return rpcErrors;
    }
}
