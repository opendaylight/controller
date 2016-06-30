/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import org.opendaylight.controller.cluster.access.ABIVersion;

public final class SuccessEnvelope<T extends RequestSuccess<?, T>> extends ResponseEnvelope<T> {
    private static final long serialVersionUID = 1L;

    SuccessEnvelope(final T message, final ABIVersion version, final long sequence, final long retry) {
        super(message, version, sequence, retry);
    }

    @Override
    SuccessEnvelopeProxy<T> createProxy() {
        return new SuccessEnvelopeProxy<>(this);
    }
}
