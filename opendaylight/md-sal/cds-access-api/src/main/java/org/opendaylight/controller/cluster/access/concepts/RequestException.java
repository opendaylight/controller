/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;

/**
 * A failure cause behind a {@link RequestFailure} to process a {@link Request}.
 *
 * @author Robert Varga
 */
@Beta
public abstract class RequestException extends Exception {
    private static final long serialVersionUID = 1L;

    protected RequestException(@Nonnull final String message) {
        super(Preconditions.checkNotNull(message));
    }

    protected RequestException(@Nonnull final String message, @Nonnull final Throwable cause) {
        super(Preconditions.checkNotNull(message), Preconditions.checkNotNull(cause));
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
