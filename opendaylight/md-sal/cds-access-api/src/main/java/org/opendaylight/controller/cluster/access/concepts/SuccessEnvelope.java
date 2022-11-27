/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import java.io.ObjectInput;

public final class SuccessEnvelope extends ResponseEnvelope<RequestSuccess<?, ?>> {
    interface SerialForm extends ResponseEnvelope.SerialForm<RequestSuccess<?, ?>, SuccessEnvelope> {
        @Override
        default SuccessEnvelope readExternal(final ObjectInput in, final long sessionId, final long txSequence,
                final RequestSuccess<?, ?> message, final long executionTimeNanos) {
            return new SuccessEnvelope(message, sessionId, txSequence, executionTimeNanos);
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public SuccessEnvelope(final RequestSuccess<?, ?> message, final long sessionId, final long txSequence,
            final long executionTimeNanos) {
        super(message, sessionId, txSequence, executionTimeNanos);
    }

    @Override
    SerialForm createProxy() {
        return new SuccessEnvelopeProxy(this);
    }
}
