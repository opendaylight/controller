/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import org.opendaylight.controller.cluster.access.concepts.RequestException;

public final class RequestTimeoutException extends RequestException {
    private static final long serialVersionUID = 1L;

    public RequestTimeoutException(final String message) {
        super(message);
    }

    @Override
    public boolean isRetriable() {
        return false;
    }
}
