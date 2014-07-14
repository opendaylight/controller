/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;

import com.google.common.base.Function;

public class TransactionCommitDeadlockException extends TransactionCommitFailedException {

    private static final long serialVersionUID = 1L;

    public static Function<Void, Exception> DEADLOCK_EXECUTOR_FUNCTION = new Function<Void, Exception>() {
        @Override
        public Exception apply(Void notUsed) {
            String message =
                    "An attempt to block on a commit callback from a write transaction was " +
                    "detected that would result in deadlock. The commit callback must be " +
                    "performed asynchronously to avoid deadlock.";

            return new TransactionCommitDeadlockException( message,
                    RpcResultBuilder.newError(ErrorType.APPLICATION, "lock-denied", message));
        }
    };

    public TransactionCommitDeadlockException(String message, final RpcError... errors) {
        super(message, errors);
    }
}
