/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * A transaction request to perform the final transaction transition, which is purging it from the protocol view,
 * meaning the frontend has no further knowledge of the transaction. The backend is free to purge any state related
 * to the transaction and responds with a {@link TransactionPurgeResponse}.
 *
 * @author Robert Varga
 */
@Beta
public final class TransactionPurgeRequest extends TransactionRequest<TransactionPurgeRequest> {
    private static final long serialVersionUID = 1L;

    public TransactionPurgeRequest(final TransactionIdentifier target, final long sequence, final ActorRef replyTo) {
        super(target, sequence, replyTo);
    }

    @Override
    protected TransactionPurgeRequestProxyV1 externalizableProxy(final ABIVersion version) {
        return new TransactionPurgeRequestProxyV1(this);
    }

    @Override
    protected TransactionPurgeRequest cloneAsVersion(final ABIVersion version) {
        return this;
    }
}