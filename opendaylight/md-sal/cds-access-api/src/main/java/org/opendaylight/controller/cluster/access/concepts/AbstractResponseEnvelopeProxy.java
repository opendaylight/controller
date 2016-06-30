/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import org.opendaylight.controller.cluster.access.ABIVersion;

abstract class AbstractResponseEnvelopeProxy<T extends Response<?, T>> extends AbstractEnvelopeProxy<T> {
    private static final long serialVersionUID = 1L;

    public AbstractResponseEnvelopeProxy() {
        // for Externalizable
    }

    AbstractResponseEnvelopeProxy(final ResponseEnvelope<T> envelope) {
        super(envelope);
    }

    @Override
    abstract ResponseEnvelope<T> createEnvelope(T message, ABIVersion version, long sequence, long retry);
}