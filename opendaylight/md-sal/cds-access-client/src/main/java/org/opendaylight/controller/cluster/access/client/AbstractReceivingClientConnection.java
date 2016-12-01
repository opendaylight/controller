/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.base.Preconditions;
import java.util.Optional;

/**
 * Implementation-internal intermediate subclass between {@link AbstractClientConnection} and two-out of three of its
 * sublcasses. It allows us to share some code.
 *
 * @author Robert Varga
 *
 * @param <T> Concrete {@link BackendInfo} type
 */
abstract class AbstractReceivingClientConnection<T extends BackendInfo> extends AbstractClientConnection<T> {
    private final T backend;
    private long nextTxSequence;

    AbstractReceivingClientConnection(final ClientActorContext context, final Long cookie, final T backend) {
        super(context, cookie);
        this.backend = Preconditions.checkNotNull(backend);
    }

    AbstractReceivingClientConnection(final AbstractReceivingClientConnection<T> oldConnection) {
        super(oldConnection);
        this.backend = oldConnection.backend;
        this.nextTxSequence = oldConnection.nextTxSequence;
    }

    @Override
    public final Optional<T> getBackendInfo() {
        return Optional.of(backend);
    }

    final T backend() {
        return backend;
    }

    final long nextTxSequence() {
        return nextTxSequence++;
    }
}
