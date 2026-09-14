/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * Internal {@link RequestException} used as poison cause when the client fails to make progress for a long time.
 * See {@link AbstractClientConnection} for details.
 */
@NonNullByDefault
final class NoProgressException extends RequestException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    NoProgressException(final long nanos) {
        super("No progress in " + TimeUnit.NANOSECONDS.toSeconds(nanos) + " seconds");
    }

    @Override
    public boolean isRetriable() {
        return false;
    }
}
