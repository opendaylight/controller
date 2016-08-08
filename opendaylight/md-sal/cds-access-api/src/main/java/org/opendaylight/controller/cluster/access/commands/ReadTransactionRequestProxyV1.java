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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Externalizable proxy for use with {@link ReadTransactionRequest}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class ReadTransactionRequestProxyV1 extends AbstractReadTransactionRequestProxyV1<ReadTransactionRequest> {
    private static final long serialVersionUID = 1L;

    public ReadTransactionRequestProxyV1() {
        // For Externalizable
    }

    ReadTransactionRequestProxyV1(final ReadTransactionRequest request) {
        super(request);
    }

    @Override
    ReadTransactionRequest createReadRequest(final TransactionIdentifier target, final long sequence,
            final ActorRef replyTo, final YangInstanceIdentifier path) {
        return new ReadTransactionRequest(target, sequence, replyTo, path);
    }
}