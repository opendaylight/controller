/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

public final class SuccessEnvelope extends ResponseEnvelope<RequestSuccess<?, ?>> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public SuccessEnvelope(final RequestSuccess<?, ?> message, final long sessionId, final long txSequence,
            final long executionTimeNanos) {
        super(message, sessionId, txSequence, executionTimeNanos);
    }

    @Override
    SE createProxy() {
        return new SE(this);
    }
}
