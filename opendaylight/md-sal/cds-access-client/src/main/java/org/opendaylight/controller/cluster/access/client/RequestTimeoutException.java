/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * A {@link RequestException} reported when a request times out.
 */
@NonNullByDefault
public final class RequestTimeoutException extends RequestException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     *
     * @param message the error message.
     */
    public RequestTimeoutException(final String message) {
        super(message);
    }

    @Override
    public boolean isRetriable() {
        return false;
    }
}
