/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.base.MoreObjects.ToStringHelper;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation-internal intermediate subclass between {@link AbstractClientConnection} and two-out of three of its
 * subclasses. It allows us to share some code.
 *
 * @author Robert Varga
 *
 * @param <T> Concrete {@link BackendInfo} type
 */
abstract class AbstractReceivingClientConnection<T extends BackendInfo> extends AbstractClientConnection<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractReceivingClientConnection.class);

    AbstractReceivingClientConnection(final ClientActorContext context, final Long cookie, final T backend) {
        super(context, cookie, backend);
    }

    AbstractReceivingClientConnection(final AbstractReceivingClientConnection<T> oldConnection) {
        super(oldConnection, targetQueueSize(oldConnection.getBackendInfo().get()));
    }

    @Override
    final void receiveResponse(final ResponseEnvelope<?> envelope) {
        if (envelope.getSessionId() != backend().getSessionId()) {
            LOG.debug("Response {} does not match session ID {}, ignoring it", envelope, backend().getSessionId());
            return;
        }

        super.receiveResponse(envelope);
    }

    private T backend() {
        return getBackendInfo().get();
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("backend", backend());
    }
}
