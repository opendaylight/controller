/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import org.opendaylight.controller.cluster.access.ABIVersion;

public abstract class ResponseEnvelope<T extends Response<?, T>> extends Envelope<T> {
    private static final long serialVersionUID = 1L;

    ResponseEnvelope(final T message, final ABIVersion version, final long sequence, final long retry) {
        super(message, version, sequence, retry);
    }

//    @Override
//    abstract AbstractResponseEnvelopeProxy<T> createProxy() {
//        return new ResponseEnvelopeProxy<>(this);
//    }
//
//    @Override
//    public Envelope<T> incrementRetry() {
//        return new ResponseEnvelope<>(getMessage(), getVersion(), getSequence(), getRetry() + 1);
//    }
}
