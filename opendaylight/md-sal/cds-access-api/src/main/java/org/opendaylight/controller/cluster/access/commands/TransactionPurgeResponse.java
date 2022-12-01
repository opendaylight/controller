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
 */
// FIXME: rename to TransactionPurgeSuccess
public final class TransactionPurgeResponse extends TransactionSuccess<TransactionPurgeResponse> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private TransactionPurgeResponse(final TransactionPurgeResponse success, final ABIVersion version) {
        super(success, version);
    }

    public TransactionPurgeResponse(final TransactionIdentifier identifier, final long sequence) {
        super(identifier, sequence);
    }

    @Override
    protected TPS externalizableProxy(final ABIVersion version) {
        return new TPS(this);
    }

    @Override
    protected TransactionPurgeResponse cloneAsVersion(final ABIVersion version) {
        return new TransactionPurgeResponse(this, version);
    }
}
