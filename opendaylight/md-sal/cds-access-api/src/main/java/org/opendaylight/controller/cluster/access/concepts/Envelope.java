/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.concepts.Immutable;

public abstract class Envelope<T extends Message<?, ?>> implements Immutable, Serializable {
    private static final long serialVersionUID = 1L;

    private final T message;
    private final ABIVersion version;
    private final long sequence;
    private final long retry;

    Envelope(final T message, final ABIVersion version, final long sequence, final long retry) {
        this.message = Preconditions.checkNotNull(message);
        this.version = Preconditions.checkNotNull(version);
        this.sequence = sequence;
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
     * Get the message sequence of this envelope.
     *
     * @return Message sequence
     */
    public long getSequence() {
        return sequence;
    }

    @VisibleForTesting
    public final ABIVersion getVersion() {
        return version;
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
        return MoreObjects.toStringHelper(Envelope.class).add("sequence", Long.toUnsignedString(sequence, 16)).
                add("retry", retry).add("message", message).toString();
    }

    final Object writeReplace() {
        return createProxy();
    }

    abstract AbstractEnvelopeProxy<T> createProxy();
}
