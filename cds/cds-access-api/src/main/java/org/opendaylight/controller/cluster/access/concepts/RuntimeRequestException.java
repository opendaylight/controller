/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Strings;

/**
 * General error raised when the recipient of a {@link Request} fails to process a request.
 */
public final class RuntimeRequestException extends RequestException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public RuntimeRequestException(final String message, final Throwable cause) {
        super(message, requireNonNull(cause));
        checkArgument(!Strings.isNullOrEmpty(message), "Exception message is mandatory");
    }

    @Override
    public boolean isRetriable() {
        return false;
    }

    @Override
    public Throwable unwrap() {
        return getCause();
    }
}
