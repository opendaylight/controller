/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;

/**
 * A failure cause behind a {@link RequestFailure} to process a {@link Request}.
 */
public abstract class RequestException extends Exception {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    protected RequestException(final @NonNull String message) {
        super(requireNonNull(message));
    }

    protected RequestException(final @NonNull String message, final @NonNull Throwable cause) {
        super(requireNonNull(message), requireNonNull(cause));
    }

    public abstract boolean isRetriable();

    /**
     * Unwraps the underlying failure. This method is overridden only in {@link RuntimeRequestException}.
     *
     * @return Underlying cause of the failure if exception is a {@link RuntimeRequestException}, or the exception
     *         itself.
     */
    public Throwable unwrap() {
        return this;
    }
}
