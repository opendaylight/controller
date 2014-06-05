/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

/**
 *
 * Failed commit of asynchronous transaction
 *
 * This exception is raised and returned when transaction commit
 * failed.
 *
 */
public class TransactionCommitFailedException extends Exception {

    private static final long serialVersionUID = -6138306275373237068L;

    protected TransactionCommitFailedException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public TransactionCommitFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public TransactionCommitFailedException(final String message) {
        super(message);
    }

}
