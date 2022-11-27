/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

abstract class AbstractEnvelopeProxy<T extends Message<?, ?>, E extends Envelope<T>>
        implements Envelope.SerialForm<T, E> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private E envelope;

    AbstractEnvelopeProxy() {
        // for Externalizable
    }

    AbstractEnvelopeProxy(final E envelope) {
        this.envelope = requireNonNull(envelope);
    }

    @Override
    public final E envelope() {
        return verifyNotNull(envelope);
    }

    @Override
    public final void setEnvelope(final E envelope) {
        this.envelope = requireNonNull(envelope);
    }

    @Override
    public final Object readResolve() {
        return envelope();
    }
}
