/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;

/**
 * A failure cause behind a {@link RequestFailure} to process a {@link Request}.
 */
@Beta
public abstract class RequestException extends Exception {
    private static final long serialVersionUID = 1L;

    protected RequestException(final String message) {
        super(message);
    }

    protected RequestException(final String message, final Exception cause) {
        super(message, cause);
    }

    public abstract boolean isRetriable();
}
