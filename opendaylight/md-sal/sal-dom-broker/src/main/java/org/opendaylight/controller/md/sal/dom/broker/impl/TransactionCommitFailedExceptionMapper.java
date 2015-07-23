/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;

/**
 * Utility exception mapper which translates Exception to {@link TransactionCommitFailedException}.
 *
 * @see ExceptionMapper
 */
public final class TransactionCommitFailedExceptionMapper
                           extends ExceptionMapper<TransactionCommitFailedException> {

    public static final TransactionCommitFailedExceptionMapper PRE_COMMIT_MAPPER = create("preCommit");

    public static final TransactionCommitFailedExceptionMapper CAN_COMMIT_ERROR_MAPPER = create("canCommit");

    public static final TransactionCommitFailedExceptionMapper COMMIT_ERROR_MAPPER = create("commit");

    private TransactionCommitFailedExceptionMapper(final String opName) {
        super( opName, TransactionCommitFailedException.class );
    }

    public static TransactionCommitFailedExceptionMapper create(final String opName) {
        return new TransactionCommitFailedExceptionMapper(opName);
    }

    @Override
    protected TransactionCommitFailedException newWithCause( final String message, final Throwable cause ) {
        return new TransactionCommitFailedException( message, cause );
    }
}