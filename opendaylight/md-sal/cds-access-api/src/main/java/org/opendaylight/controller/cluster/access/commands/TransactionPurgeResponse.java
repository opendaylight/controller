/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Successful reply to a {@link TransactionPurgeRequest}.
 *
 * @author Robert Varga
 */
public final class TransactionPurgeResponse extends TransactionSuccess<TransactionPurgeResponse> {
    private static final long serialVersionUID = 1L;

    public TransactionPurgeResponse(final TransactionIdentifier identifier, final long sequence) {
        super(identifier, sequence);
    }

    @Override
    protected AbstractTransactionSuccessProxy<TransactionPurgeResponse> externalizableProxy(
            final ABIVersion version) {
        return new TransactionPurgeResponseProxyV1(this);
    }

    @Override
    protected TransactionPurgeResponse cloneAsVersion(final ABIVersion version) {
        return this;
    }
}
