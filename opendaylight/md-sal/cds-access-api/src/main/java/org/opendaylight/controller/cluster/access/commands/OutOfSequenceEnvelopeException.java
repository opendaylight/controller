/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.annotations.Beta;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * A {@link RequestException} indicating that the backend has received a RequestEnvelope whose sequence does not match
 * the next expected sequence. This can happen during leader transitions, when a part of the stream is rejected because
 * the backend is not the leader and it transitions to being a leader with old stream messages still being present.
 *
 * @author Robert Varga
 */
@Beta
public final class OutOfSequenceEnvelopeException extends RequestException {
    private static final long serialVersionUID = 1L;

    public OutOfSequenceEnvelopeException(final long expectedEnvelope) {
        super("Expecting envelope " + Long.toUnsignedString(expectedEnvelope));
    }

    @Override
    public boolean isRetriable() {
        return true;
    }
}
