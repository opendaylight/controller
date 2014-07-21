/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import com.google.common.base.Preconditions;

/**
 *
 * Failure of asynchronous transaction commit caused by invalid data.
 *
 * This exception is raised and returned when transaction commit
 * failed, because other data submitted via transactions
 *
 *  Clients usually are not able recover from this error condition by
 *  retrieving same transaction, since data introduced by this transaction
 *  are invalid.
 *
 */
public class DataValidationFailedException extends TransactionCommitFailedException {

    private static final long serialVersionUID = 1L;

    private Path<?> path;

    private Class<? extends Path<?>> pathType;

    public <P extends Path<P>> DataValidationFailedException(final Class<P> pathType,final P path,
                                                             final String message, final Throwable cause) {
        super(message, cause, RpcResultBuilder.newError(ErrorType.APPLICATION, "invalid-value", message, null,
                                                        path != null ? path.toString() : null, cause));
        this.pathType = Preconditions.checkNotNull(pathType, "path type must not be null");
        this.path = Preconditions.checkNotNull(path,"path must not be null.");
    }

    public  <P extends Path<P>> DataValidationFailedException(final Class<P> pathType,final P path,
                                                              final String message) {
        this(pathType, path, message, null);
    }

    public final Path<?> getPath() {
        return path;
    }

    public final Class<? extends Path<?>> getPathType() {
        return pathType;
    }

}
