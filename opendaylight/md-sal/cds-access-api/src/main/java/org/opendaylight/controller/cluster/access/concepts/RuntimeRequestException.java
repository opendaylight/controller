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
import com.google.common.base.Strings;

/**
 * General error raised when the recipient of a {@link Request} fails to process a request.
 *
 * @author Robert Varga
 */
@Beta
public final class RuntimeRequestException extends RequestException {
    private static final long serialVersionUID = 1L;

    public RuntimeRequestException(final String message, final Throwable cause) {
        super(message, Preconditions.checkNotNull(cause));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(message), "Exception message is mandatory");
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
