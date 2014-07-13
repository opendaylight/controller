/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.api;

import java.util.Arrays;
import java.util.List;

import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;

import com.google.common.collect.ImmutableList;

/**
 * A general base exception for an operation failure.
 *
 * @author Thomas Pantelis
 */
public class OperationFailedException extends Exception {

    private static final long serialVersionUID = 1L;

    private final List<RpcError> errorList;

    public OperationFailedException(final String message, final RpcError... errors) {
        this(message, null, errors);
    }

    public OperationFailedException(final String message, final Throwable cause,
                              final RpcError... errors) {
        super(message, cause);

        if( errors != null && errors.length > 0 ) {
            errorList = ImmutableList.<RpcError>builder().addAll( Arrays.asList( errors ) ).build();
        }
        else {
            // Add a default RpcError.
            errorList = ImmutableList.of(RpcResultBuilder.newError(ErrorType.APPLICATION, null,
                    getMessage(), null, null, getCause()));
        }
    }

    /**
     * Returns additional error information about this exception.
     *
     * @return a List of RpcErrors. There is always at least one RpcError.
     */
    public List<RpcError> getErrorList() {
        return errorList;
    }

    @Override
    public String getMessage() {
        return new StringBuilder( super.getMessage() ).append(", errors: ").append( errorList ).toString();
    }
}
