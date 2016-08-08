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
 * Externalizable proxy for use with {@link TransactionPreCommitRequest}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class TransactionPreCommitRequestProxyV1 extends AbstractTransactionRequestProxy<TransactionPreCommitRequest> {
    private static final long serialVersionUID = 1L;

    public TransactionPreCommitRequestProxyV1() {
        // For Externalizable
    }

    TransactionPreCommitRequestProxyV1(final TransactionPreCommitRequest request) {
        super(request);
    }

    @Override
    protected TransactionPreCommitRequest createRequest(final TransactionIdentifier target, final long sequence,
            final ActorRef replyTo) {
        return new TransactionPreCommitRequest(target, sequence, replyTo);
    }
}
