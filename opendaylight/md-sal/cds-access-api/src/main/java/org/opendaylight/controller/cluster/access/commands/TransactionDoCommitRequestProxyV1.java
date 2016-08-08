/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Externalizable proxy for use with {@link TransactionDoCommitRequest}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class TransactionDoCommitRequestProxyV1 extends AbstractTransactionRequestProxy<TransactionDoCommitRequest> {
    private static final long serialVersionUID = 1L;

    public TransactionDoCommitRequestProxyV1() {
        // For Externalizable
    }

    TransactionDoCommitRequestProxyV1(final TransactionDoCommitRequest request) {
        super(request);
    }

    @Override
    protected TransactionDoCommitRequest createRequest(final TransactionIdentifier target, final long sequence,
            final ActorRef replyTo) {
        return new TransactionDoCommitRequest(target, sequence, replyTo);
    }
}
