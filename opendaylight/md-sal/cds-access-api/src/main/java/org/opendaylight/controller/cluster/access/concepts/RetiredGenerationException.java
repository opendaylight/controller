/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

/**
 * General error raised when the recipient of a {@link Request} determines that the request contains
 * a {@link ClientIdentifier} which corresponds to an outdated generation.
 */
public final class RetiredGenerationException extends RequestException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public RetiredGenerationException(final long originatingGeneration, final long newGeneration) {
        super("Originating generation " + Long.toUnsignedString(originatingGeneration) + " was superseded by "
            + Long.toUnsignedString(newGeneration));
    }

    @Override
    public boolean isRetriable() {
        return false;
    }
}
