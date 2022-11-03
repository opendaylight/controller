/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import java.io.Serial;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Successful reply to a {@link SkipTransactionsRequest}.
 */
public final class SkipTransactionsResponse extends TransactionSuccess<SkipTransactionsResponse> {
    @Serial
    private static final long serialVersionUID = 1L;

    public SkipTransactionsResponse(final TransactionIdentifier identifier, final long sequence) {
        super(identifier, sequence);
    }

    @Override
    protected AbstractTransactionSuccessProxy<SkipTransactionsResponse> externalizableProxy(
            final ABIVersion version) {
        return new SkipTransactionsResponseProxyV1(this);
    }

    @Override
    protected SkipTransactionsResponse cloneAsVersion(final ABIVersion version) {
        return this;
    }
}
