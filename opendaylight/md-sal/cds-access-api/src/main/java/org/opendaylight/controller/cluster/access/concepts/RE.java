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
import org.opendaylight.controller.akka.queue.Envelope;
import org.opendaylight.controller.akka.queue.Request;
import org.opendaylight.controller.akka.queue.RequestEnvelope;

/**
 * Serialization proxy for {@link RequestEnvelope}.
 *
 * @deprecated Superseded serial form.
 */
@Deprecated(since = "9.0.0", forRemoval = true)
final class RE implements Envelope.SerialForm<Request<?, ?>, RequestEnvelope> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private RequestEnvelope envelope;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public RE() {
        // for Externalizable
    }


    @Override
    public RequestEnvelope envelope() {
        return verifyNotNull(envelope);
    }

    @Override
    public void setEnvelope(final RequestEnvelope envelope) {
        this.envelope = requireNonNull(envelope);
    }

    @Override
    public RequestEnvelope readExternal(final ObjectInput in, final long sessionId, final long txSequence,
            final Request<?, ?> message) {
        return new RequestEnvelope(message, sessionId, txSequence);
    }

    @Override
    public Object readResolve() {
        return envelope();
    }
}
