/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

final class SuccessEnvelopeProxy extends AbstractResponseEnvelopeProxy<RequestSuccess<?, ?>> {
    private static final long serialVersionUID = 1L;

    // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
    // be able to create instances via reflection.
    @SuppressWarnings("checkstyle:RedundantModifier")
    public SuccessEnvelopeProxy() {
        // for Externalizable
    }

    SuccessEnvelopeProxy(final SuccessEnvelope envelope) {
        super(envelope);
    }

    @Override
    ResponseEnvelope<RequestSuccess<?, ?>> createEnvelope(final RequestSuccess<?, ?> message, final long sessionId,
            final long txSequence, final long executionTimeNanos) {
        return new SuccessEnvelope(message, sessionId, txSequence, executionTimeNanos);
    }
}
