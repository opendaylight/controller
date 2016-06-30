/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import org.opendaylight.controller.cluster.access.ABIVersion;

final class SuccessEnvelopeProxy<T extends RequestSuccess<?, T>> extends AbstractResponseEnvelopeProxy<T> {
    private static final long serialVersionUID = 1L;

    public SuccessEnvelopeProxy() {
        // for Externalizable
    }

    SuccessEnvelopeProxy(final SuccessEnvelope<T> envelope) {
        super(envelope);
    }

    @Override
    SuccessEnvelope<T> createEnvelope(final T message, final ABIVersion version, final long sequence,
            final long retry) {
        return new SuccessEnvelope<>(message, version, sequence, retry);
    }
}
