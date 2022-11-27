/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableObjects;

public abstract class ResponseEnvelope<T extends Response<?, ?>> extends Envelope<T> {
    interface SerialForm<T extends Response<?, ?>, E extends ResponseEnvelope<T>> extends Envelope.SerialForm<T, E> {
        @Override
        default void writeExternal(final ObjectOutput out, final @NonNull E envelope) throws IOException {
            Envelope.SerialForm.super.writeExternal(out, envelope);
            WritableObjects.writeLong(out, envelope.getExecutionTimeNanos());
        }

        @Override
        default E readExternal(final ObjectInput in, final long sessionId, final long txSequence, final T message)
                throws IOException {
            return readExternal(in, sessionId, txSequence, message, WritableObjects.readLong(in));
        }

        E readExternal(ObjectInput in, long sessionId, long txSequence, T message, long executionTimeNanos);
    }

    private static final long serialVersionUID = 1L;

    private final long executionTimeNanos;

    ResponseEnvelope(final T message, final long sessionId, final long txSequence, final long executionTimeNanos) {
        super(message, sessionId, txSequence);
        checkArgument(executionTimeNanos >= 0, "Negative executionTime");
        this.executionTimeNanos = executionTimeNanos;
    }

    /**
     * Return the time the request took to execute in nanoseconds. This may not reflect the actual CPU time, but rather
     * a measure of the complexity involved in servicing the original request.
     *
     * @return Time the request took to execute in nanoseconds
     */
    public final long getExecutionTimeNanos() {
        return executionTimeNanos;
    }
}
