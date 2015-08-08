/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;

/**
*
* Failure of asynchronous transaction commit caused by failure
* of optimistic locking.
*
* This exception is raised and returned when transaction commit
* failed, because other transaction finished successfully
* and modified same data as failed transaction.
*
*  Clients may recover from this error condition by
*  retrieving current state and submitting new updated
*  transaction.
*
*/
public class OptimisticLockFailedException extends TransactionCommitFailedException {

    private static final long serialVersionUID = 1L;

    public OptimisticLockFailedException(final String message, final Throwable cause) {
        super(message, cause, RpcResultBuilder.newError(ErrorType.APPLICATION, "resource-denied",
                                                        message, null, null, cause));
    }

    public OptimisticLockFailedException(final String message) {
        this(message, null);
    }

}
