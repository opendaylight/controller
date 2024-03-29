/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import java.io.ObjectInput;

/**
 * Serialization proxy for {@link FailureEnvelope}.
 */
final class FE implements ResponseEnvelope.SerialForm<RequestFailure<?, ?>, FailureEnvelope> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private FailureEnvelope envelope;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public FE() {
        // for Externalizable
    }

    FE(final FailureEnvelope envelope) {
        this.envelope = requireNonNull(envelope);
    }

    @Override
    public FailureEnvelope envelope() {
        return verifyNotNull(envelope);
    }

    @Override
    public void setEnvelope(final FailureEnvelope envelope) {
        this.envelope = requireNonNull(envelope);
    }

    @Override
    public FailureEnvelope readExternal(final ObjectInput in, final long sessionId, final long txSequence,
            final RequestFailure<?, ?> message, final long executionTimeNanos) {
        return new FailureEnvelope(message, sessionId, txSequence, executionTimeNanos);
    }

    @Override
    public Object readResolve() {
        return envelope();
    }
}
