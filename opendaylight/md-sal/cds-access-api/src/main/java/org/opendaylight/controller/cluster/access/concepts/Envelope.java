/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import org.opendaylight.yangtools.concepts.Immutable;

public abstract class Envelope<T extends Message<?, ?>> implements Immutable, Serializable {
    private static final long serialVersionUID = 1L;

    private final T message;
    private final long txSequence;
    private final long retry;

    Envelope(final T message, final long txSequence, final long retry) {
        this.message = Preconditions.checkNotNull(message);
        this.txSequence = txSequence;
        this.retry = retry;
    }

    /**
     * Get the enclosed message
     *
     * @return enclose message
     */
    public T getMessage() {
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
     * Get the message retry counter.
     *
     * @return Retry counter
     */
    public long getRetry() {
        return retry;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(Envelope.class).add("txSequence", Long.toHexString(txSequence))
                .add("retry", retry).add("message", message).toString();
    }

    final Object writeReplace() {
        return createProxy();
    }

    abstract AbstractEnvelopeProxy<T> createProxy();
}
