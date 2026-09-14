/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import java.time.Instant;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * Internal {@link RequestException} used as the cause for {@link ClientActorBehavior#haltClient(Throwable)} to indicate
 * the client has been terminated.
 */
@NonNullByDefault
public final class TerminatedException extends RequestException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    TerminatedException(final Instant terminationInstant) {
        super("Terminated at " + terminationInstant.toString());
    }

    @Override
    public boolean isRetriable() {
        return false;
    }
}
