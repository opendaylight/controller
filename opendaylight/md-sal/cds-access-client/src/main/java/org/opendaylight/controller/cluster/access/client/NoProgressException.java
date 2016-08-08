/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * Internal {@link RequestException} used as poison cause when the client fails to make progress for a long time.
 * See {@link SequencedQueue} for details.
 *
 * @author Robert Varga
 */
final class NoProgressException extends RequestException {
    private static final long serialVersionUID = 1L;

    protected NoProgressException(final long nanos) {
        super(String.format("No progress in %s seconds", TimeUnit.NANOSECONDS.toSeconds(nanos)));
    }

    @Override
    public boolean isRetriable() {
        return false;
    }
}
