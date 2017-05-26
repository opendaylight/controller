/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.opendaylight.yangtools.yang.common.RpcError;

/**
 * An exception for a failed read.
 */
public class ReadFailedException extends OperationFailedException {

    private static final long serialVersionUID = 1L;

    public static final ExceptionMapper<ReadFailedException> MAPPER =
            new ExceptionMapper<ReadFailedException>("read", ReadFailedException.class) {
                @Override
                protected ReadFailedException newWithCause(String message, Throwable cause) {
                    return new ReadFailedException(message, cause);
                }
    };

    public ReadFailedException(String message, RpcError... errors) {
        super(message, errors);
    }

    public ReadFailedException(String message, Throwable cause, RpcError... errors) {
        super(message, cause, errors);
    }
}
