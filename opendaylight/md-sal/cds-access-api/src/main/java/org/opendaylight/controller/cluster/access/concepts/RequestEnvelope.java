/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import akka.actor.ActorRef;
import java.io.ObjectInput;

public final class RequestEnvelope extends Envelope<Request<?, ?>> {
    interface SerialForm extends Envelope.SerialForm<Request<?, ?>, RequestEnvelope> {
        @Override
        default RequestEnvelope readExternal(final ObjectInput in, final long sessionId, final long txSequence,
                final Request<?, ?> message) {
            return new RequestEnvelope(message, sessionId, txSequence);
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public RequestEnvelope(final Request<?, ?> message, final long sessionId, final long txSequence) {
        super(message, sessionId, txSequence);
    }

    @Override
    RE createProxy() {
        return new RE(this);
    }

    @Override
    RequestEnvelopeProxy legacyProxy() {
        return new RequestEnvelopeProxy(this);
    }

    /**
     * Respond to this envelope with a {@link RequestFailure} caused by specified {@link RequestException}.
     *
     * @param cause Cause of this {@link RequestFailure}
     * @param executionTimeNanos Time to execute the request, in nanoseconds
     * @throws NullPointerException if cause is null
     */
    public void sendFailure(final RequestException cause, final long executionTimeNanos) {
        sendResponse(new FailureEnvelope(getMessage().toRequestFailure(cause), getSessionId(), getTxSequence(),
            executionTimeNanos));
    }

    /**
     * Respond to this envelope with a {@link RequestSuccess}.
     *
     * @param success Successful response
     * @throws NullPointerException if success is null
     */
    public void sendSuccess(final RequestSuccess<?, ?> success, final long executionTimeNanos) {
        sendResponse(newSuccessEnvelope(success, executionTimeNanos));
    }

    /**
     * Creates a successful ResponseEnvelope that wraps the given successful Request response message.
     *
     * @param success the successful Request response message
     * @param executionTimeNanos the execution time of the request
     * @return a {@link ResponseEnvelope} instance
     */
    public ResponseEnvelope<?> newSuccessEnvelope(final RequestSuccess<?, ?> success, final long executionTimeNanos) {
        return new SuccessEnvelope(success, getSessionId(), getTxSequence(), executionTimeNanos);
    }

    private void sendResponse(final ResponseEnvelope<?> envelope) {
        getMessage().getReplyTo().tell(envelope, ActorRef.noSender());
    }
}
