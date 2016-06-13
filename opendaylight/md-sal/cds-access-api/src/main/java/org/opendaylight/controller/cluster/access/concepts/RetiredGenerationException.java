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
 * General error raised when the recipient of a {@link Request} determines that the request contains
 * a {@link ClientIdentifier} which corresponds to an outdated generation.
 *
 * @author Robert Varga
 */
@Beta
public final class RetiredGenerationException extends RequestException {
    private static final long serialVersionUID = 1L;

    public RetiredGenerationException(final long newGeneration) {
        super("Originating generation was superseded by " + Long.toUnsignedString(newGeneration));
    }

    @Override
    public boolean isRetriable() {
        return false;
    }
}
