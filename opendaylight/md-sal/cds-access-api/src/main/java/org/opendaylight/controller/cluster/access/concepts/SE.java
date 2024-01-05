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
import org.opendaylight.controller.akka.queue.RequestSuccess;
import org.opendaylight.controller.akka.queue.ResponseEnvelope;
import org.opendaylight.controller.akka.queue.SuccessEnvelope;

/**
 * Serialization proxy for {@link SuccessEnvelope}.
 *
 * @deprecated Superseded serial form.
 */
@Deprecated(since = "9.0.0", forRemoval = true)
final class SE implements ResponseEnvelope.SerialForm<RequestSuccess<?, ?>, SuccessEnvelope> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private SuccessEnvelope envelope;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public SE() {
        // for Externalizable
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
