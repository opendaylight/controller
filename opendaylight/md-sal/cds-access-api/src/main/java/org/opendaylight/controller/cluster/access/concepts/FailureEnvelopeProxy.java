/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

final class FailureEnvelopeProxy extends AbstractResponseEnvelopeProxy<RequestFailure<?, ?>> {
    private static final long serialVersionUID = 1L;

    // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to be
    // able to create instances via reflection.
    @SuppressWarnings("checkstyle:RedundantModifier")
    public FailureEnvelopeProxy() {
        // for Externalizable
    }

    FailureEnvelopeProxy(final FailureEnvelope envelope) {
        super(envelope);
    }

    @Override
    ResponseEnvelope<RequestFailure<?, ?>> createEnvelope(final RequestFailure<?, ?> message, final long sessionId,
            final long txSequence, final long executionTimeNanos) {
        return new FailureEnvelope(message, sessionId, txSequence, executionTimeNanos);
    }
}
