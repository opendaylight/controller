/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Externalizable proxy for use with {@link TransactionPurgeResponse}. It implements the initial (Boron)
 * serialization format.
 *
 * @author Robert Varga
 */
final class TransactionPurgeResponseProxyV1 extends AbstractTransactionSuccessProxy<TransactionPurgeResponse> {
    private static final long serialVersionUID = 1L;

    // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
    // be able to create instances via reflection.
    @SuppressWarnings("checkstyle:RedundantModifier")
    public TransactionPurgeResponseProxyV1() {
        // For Externalizable
    }

    TransactionPurgeResponseProxyV1(final TransactionPurgeResponse success) {
        super(success);
    }

    @Override
    protected TransactionPurgeResponse createSuccess(final TransactionIdentifier target, final long sequence) {
        return new TransactionPurgeResponse(target, sequence);
    }
}
