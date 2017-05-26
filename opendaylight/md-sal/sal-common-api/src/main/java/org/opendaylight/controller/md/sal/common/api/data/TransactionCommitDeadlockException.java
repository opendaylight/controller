/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.api.data;

import com.google.common.base.Supplier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * A type of TransactionCommitFailedException that indicates a situation that would result in a
 * threading deadlock. This can occur if a caller that submits a write transaction tries to perform
 * a blocking call via one of the <code>get</code> methods on the returned ListenableFuture. Callers
 * should process the commit result asynchronously (via Futures#addCallback) to ensure deadlock
 * won't occur.
 *
 * @author Thomas Pantelis
 */
public class TransactionCommitDeadlockException extends TransactionCommitFailedException {
    private static final long serialVersionUID = 1L;
    private static final String DEADLOCK_MESSAGE =
            "An attempt to block on a ListenableFuture via a get method from a write " +
            "transaction submit was detected that would result in deadlock. The commit " +
            "result must be obtained asynchronously, e.g. via Futures#addCallback, to avoid deadlock.";
    private static final RpcError DEADLOCK_RPCERROR = RpcResultBuilder.newError(ErrorType.APPLICATION, "lock-denied", DEADLOCK_MESSAGE);

    public static final Supplier<Exception> DEADLOCK_EXCEPTION_SUPPLIER = new Supplier<Exception>() {
        @Override
        public Exception get() {
            return new TransactionCommitDeadlockException(DEADLOCK_MESSAGE, DEADLOCK_RPCERROR);
        }
    };

    public TransactionCommitDeadlockException(final String message, final RpcError... errors) {
        super(message, errors);
    }
}
