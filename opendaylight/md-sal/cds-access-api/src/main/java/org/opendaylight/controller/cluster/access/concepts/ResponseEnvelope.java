/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.base.Preconditions;

public abstract class ResponseEnvelope<T extends Response<?, ?>> extends Envelope<T> {
    private static final long serialVersionUID = 1L;

    private final long executionTimeNanos;

    ResponseEnvelope(final T message, final long sessionId, final long txSequence, final long executionTimeNanos) {
        super(message, sessionId, txSequence);
        Preconditions.checkArgument(executionTimeNanos >= 0);
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

    @Override
    abstract AbstractResponseEnvelopeProxy<T> createProxy();
}
