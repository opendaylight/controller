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
 * Serialization proxy for {@link SuccessEnvelope}.
 */
final class SE implements SuccessEnvelope.SerialForm {
    private static final long serialVersionUID = 1L;

    private SuccessEnvelope envelope;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public SE() {
        // for Externalizable
    }

    SE(final SuccessEnvelope envelope) {
        this.envelope = requireNonNull(envelope);
    }

    @Override
    public SuccessEnvelope envelope() {
        return verifyNotNull(envelope);
    }

    @Override
    public void setEnvelope(final SuccessEnvelope envelope) {
        this.envelope = requireNonNull(envelope);
    }

    @Override
    public SuccessEnvelope readExternal(final ObjectInput in, final long sessionId, final long txSequence,
            final RequestSuccess<?, ?> message, final long executionTimeNanos) {
        return new SuccessEnvelope(message, sessionId, txSequence, executionTimeNanos);
    }

    @Override
    public Object readResolve() {
        return envelope();
    }
}
