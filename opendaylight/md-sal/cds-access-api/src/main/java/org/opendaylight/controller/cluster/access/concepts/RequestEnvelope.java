/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.ABIVersion;

public final class RequestEnvelope<T extends Request<?, T>> extends Envelope<T> {
    private static final long serialVersionUID = 1L;

    public RequestEnvelope(final T message, final ABIVersion version, final long sequence, final long retry) {
        super(message, version, sequence, retry);
    }

    @Override
    AbstractEnvelopeProxy<T> createProxy() {
        return new RequestEnvelopeProxy<>(this);
    }

    /**
     * Return a message which will have the retry counter incremented by one.
     *
     * @return A message with the specified retry counter
     */
    public @Nonnull RequestEnvelope<T> incrementRetry() {
        return new RequestEnvelope<>(getMessage(), getVersion(), getSequence(), getRetry() + 1);
    }
}
