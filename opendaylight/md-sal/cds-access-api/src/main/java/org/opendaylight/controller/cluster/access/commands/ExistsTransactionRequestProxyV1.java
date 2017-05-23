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
 * Externalizable proxy for use with {@link ExistsTransactionRequest}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class ExistsTransactionRequestProxyV1 extends
        AbstractReadPathTransactionRequestProxyV1<ExistsTransactionRequest> {
    private static final long serialVersionUID = 1L;

    // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
    // be able to create instances via reflection.
    @SuppressWarnings("checkstyle:RedundantModifier")
    public ExistsTransactionRequestProxyV1() {
        // For Externalizable
    }

    ExistsTransactionRequestProxyV1(final ExistsTransactionRequest request) {
        super(request);
    }

    @Override
    ExistsTransactionRequest createReadPathRequest(final TransactionIdentifier target, final long sequence,
            final ActorRef replyTo, final YangInstanceIdentifier path, final boolean snapshotOnly) {
        return new ExistsTransactionRequest(target, sequence, replyTo, path, snapshotOnly);
    }
}
