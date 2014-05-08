/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 *
 * Utility exception mapper which translates {@link Exception}
 * to {@link TransactionCommitFailedException}.
 *
 * This mapper is intended to be used with {@link com.google.common.util.concurrent.Futures#makeChecked(com.google.common.util.concurrent.ListenableFuture, Function)}
 * <ul>
 * <li>if exception is {@link TransactionCommitFailedException} or one of its subclasses returns original exception.
 * <li>if exception is {@link ExecutionException} and cause is  {@link TransactionCommitFailedException} return cause
 * <li>otherwise returns {@link TransactionCommitFailedException} with original exception as a cause.
 * </ul>
 *
 */

final class TransactionCommitFailedExceptionMapper implements
        Function<Exception, TransactionCommitFailedException> {

    static final TransactionCommitFailedExceptionMapper PRE_COMMIT_MAPPER = create("canCommit");

    static final TransactionCommitFailedExceptionMapper CAN_COMMIT_ERROR_MAPPER = create("preCommit");

    static final TransactionCommitFailedExceptionMapper COMMIT_ERROR_MAPPER = create("commit");

    private final String opName;

    private TransactionCommitFailedExceptionMapper(final String opName) {
        this.opName = Preconditions.checkNotNull(opName);
    }

    public static final TransactionCommitFailedExceptionMapper create(final String opName) {
        return new TransactionCommitFailedExceptionMapper(opName);
    }

    @Override
    public TransactionCommitFailedException apply(final Exception e) {
        // If excetion is TransactionCommitFailedException
        // we reuse it directly.
        if (e instanceof TransactionCommitFailedException) {
            return (TransactionCommitFailedException) e;
        }
        // If error is ExecutionException which was caused by cause of
        // TransactionCommitFailedException
        // we reuse original cause
        if (e instanceof ExecutionException && e.getCause() instanceof TransactionCommitFailedException) {
            return (TransactionCommitFailedException) e.getCause();
        }
        if (e instanceof InterruptedException) {
            return new TransactionCommitFailedException(opName + " failed - DOMStore was interupted.", e);
        }
        // Otherwise we are using new exception, with original cause
        return new TransactionCommitFailedException(opName + " failed", e);
    }
}