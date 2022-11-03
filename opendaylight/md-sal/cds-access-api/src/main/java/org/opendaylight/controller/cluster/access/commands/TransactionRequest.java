/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import java.io.Serial;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Abstract base class for {@link Request}s involving specific transaction. This class is visible outside of this
 * package solely for the ability to perform a unified instanceof check.
 *
 * @param <T> Message type
 */
public abstract class TransactionRequest<T extends TransactionRequest<T>> extends Request<TransactionIdentifier, T> {
    @Serial
    private static final long serialVersionUID = 1L;

    TransactionRequest(final TransactionIdentifier identifier, final long sequence, final ActorRef replyTo) {
        super(identifier, sequence, replyTo);
    }

    TransactionRequest(final T request, final ABIVersion version) {
        super(request, version);
    }

    @Override
    public final TransactionFailure toRequestFailure(final RequestException cause) {
        return new TransactionFailure(getTarget(), getSequence(), cause);
    }

    @Override
    protected abstract AbstractTransactionRequestProxy<T> externalizableProxy(ABIVersion version);
}
