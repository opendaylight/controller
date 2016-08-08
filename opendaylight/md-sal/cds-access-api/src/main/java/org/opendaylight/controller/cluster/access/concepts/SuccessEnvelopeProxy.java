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

    public SuccessEnvelopeProxy() {
        // for Externalizable
    }

    SuccessEnvelopeProxy(final SuccessEnvelope envelope) {
        super(envelope);
    }

    @Override
    SuccessEnvelope createEnvelope(final RequestSuccess<?, ?> message, final long sessionId, final long txSequence) {
        return new SuccessEnvelope(message, sessionId, txSequence);
    }
}
