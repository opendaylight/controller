/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Immutable;

public abstract class Envelope<T extends Message<?, ?>> implements Immutable, Serializable {
    private static final long serialVersionUID = 1L;

    private final @NonNull T message;
    private final long txSequence;
    private final long sessionId;

    Envelope(final T message, final long sessionId, final long txSequence) {
        this.message = requireNonNull(message);
        this.sessionId = sessionId;
        this.txSequence = txSequence;
    }

    /**
     * Get the enclosed message.
     *
     * @return enclose message
     */
    public @NonNull T getMessage() {
        return message;
    }

    /**
     * Get the message transmission sequence of this envelope.
     *
     * @return Message sequence
     */
    public long getTxSequence() {
        return txSequence;
    }

    /**
     * Get the session identifier.
     *
     * @return Session identifier
     */
    public long getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(Envelope.class).add("sessionId", Long.toHexString(sessionId))
                .add("txSequence", Long.toHexString(txSequence)).add("message", message).toString();
    }

    final Object writeReplace() {
        return createProxy();
    }

    abstract AbstractEnvelopeProxy<T> createProxy();
}
