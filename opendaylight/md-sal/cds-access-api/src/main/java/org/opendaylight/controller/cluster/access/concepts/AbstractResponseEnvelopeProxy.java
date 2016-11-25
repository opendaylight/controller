/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.concepts.WritableObjects;

abstract class AbstractResponseEnvelopeProxy<T extends Response<?, ?>> extends AbstractEnvelopeProxy<T> {
    private static final long serialVersionUID = 1L;

    private long executionTimeNanos;

    AbstractResponseEnvelopeProxy() {
        // for Externalizable
    }

    AbstractResponseEnvelopeProxy(final ResponseEnvelope<T> envelope) {
        super(envelope);
        this.executionTimeNanos = envelope.getExecutionTimeNanos();
    }

    @Override
    public final void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        WritableObjects.writeLong(out, executionTimeNanos);
    }

    @Override
    public final void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        executionTimeNanos = WritableObjects.readLong(in);
    }

    @Override
    final ResponseEnvelope<T> createEnvelope(final T message, final long sessionId, final long txSequence) {
        return createEnvelope(message, sessionId, txSequence, executionTimeNanos);
    }

    abstract ResponseEnvelope<T> createEnvelope(T message, long sessionId, long txSequence, long executionTimeNanos);
}
