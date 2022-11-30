/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

@Deprecated(since = "7.0.0", forRemoval = true)
abstract class AbstractResponseEnvelopeProxy<T extends Response<?, ?>, E extends ResponseEnvelope<T>>
        extends AbstractEnvelopeProxy<T, E> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    AbstractResponseEnvelopeProxy() {
        // for Externalizable
    }

    AbstractResponseEnvelopeProxy(final E envelope) {
        super(envelope);
    }
}
