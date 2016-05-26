/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Abstract base class for {@link Request}s involving specific transactions local to a member node. These transactions
 * take advantage of isolation provided by the DataTree, performing transaction modifications on the frontend.
 *
 * @author Robert Varga
 *
 * @param <T> Message type
 */
abstract class AbstractLocalTransactionRequest<T extends AbstractLocalTransactionRequest<T>> extends TransactionRequest<T> {
    private static final long serialVersionUID = 1L;

    AbstractLocalTransactionRequest(final TransactionIdentifier identifier, final long sequence, final ActorRef replyTo) {
        super(identifier, sequence, replyTo);
    }

    @Override
    protected final AbstractTransactionRequestProxy<T> externalizableProxy(final ABIVersion version) {
        throw new UnsupportedOperationException("Local transaction request should never be serialized");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected final T cloneAsVersion(final ABIVersion version) {
        // These messages cannot be serialized, hence we this method is a no-op
        return (T)this;
    }
}
