/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import akka.actor.ActorRef;

public final class RequestEnvelope extends Envelope<Request<?, ?>> {
    private static final long serialVersionUID = 1L;

    public RequestEnvelope(final Request<?, ?> message, final long sequence, final long retry) {
        super(message, sequence, retry);
    }

    @Override
    RequestEnvelopeProxy createProxy() {
        return new RequestEnvelopeProxy(this);
    }

    /**
     * Respond to this envelope with a {@link RequestFailure} caused by specified {@link RequestException}.
     *
     * @param cause Cause of this {@link RequestFailure}
     * @throws NullPointerException if cause is null
     */
    public void sendFailure(final RequestException cause) {
        sendResponse(new FailureEnvelope(getMessage().toRequestFailure(cause), getTxSequence(), getRetry()));
    }

    /**
     * Respond to this envelope with a {@link RequestSuccess}.
     *
     * @param success Successful response
     * @throws NullPointerException if success is null
     */
    public void sendSuccess(final RequestSuccess<?, ?> success) {
        sendResponse(new SuccessEnvelope(success, getTxSequence(), getRetry()));
    }

    private void sendResponse(final ResponseEnvelope<?> envelope) {
        getMessage().getReplyTo().tell(envelope, ActorRef.noSender());
    }
}
