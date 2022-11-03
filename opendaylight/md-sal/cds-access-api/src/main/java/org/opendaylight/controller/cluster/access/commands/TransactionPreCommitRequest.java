/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import java.io.Serial;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * A transaction request to perform the second, preCommit, step of the three-phase commit protocol.
 */
public final class TransactionPreCommitRequest extends TransactionRequest<TransactionPreCommitRequest> {
    @Serial
    private static final long serialVersionUID = 1L;

    public TransactionPreCommitRequest(final TransactionIdentifier target, final long sequence,
            final ActorRef replyTo) {
        super(target, sequence, replyTo);
    }

    @Override
    protected TransactionPreCommitRequestProxyV1 externalizableProxy(final ABIVersion version) {
        return new TransactionPreCommitRequestProxyV1(this);
    }

    @Override
    protected TransactionPreCommitRequest cloneAsVersion(final ABIVersion version) {
        return this;
    }
}
