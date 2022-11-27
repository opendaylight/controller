/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.annotations.Beta;
import java.io.ObjectInput;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Successful reply to a {@link SkipTransactionsRequest}.
 */
// FIXME: rename to SkipTransactionsSuccess
@Beta
public final class SkipTransactionsResponse extends TransactionSuccess<SkipTransactionsResponse> {
    interface SerialForm extends TransactionSuccess.SerialForm<SkipTransactionsResponse> {
        @Override
        default SkipTransactionsResponse readExternal(final ObjectInput in, final TransactionIdentifier target,
                final long sequence) {
            return new SkipTransactionsResponse(target, sequence);
        }
    }

    private static final long serialVersionUID = 1L;

    private SkipTransactionsResponse(final SkipTransactionsResponse success, final ABIVersion version) {
        super(success, version);
    }

    public SkipTransactionsResponse(final TransactionIdentifier identifier, final long sequence) {
        super(identifier, sequence);
    }

    @Override
    protected SerialForm externalizableProxy(final ABIVersion version) {
        return ABIVersion.MAGNESIUM.lt(version) ? new STS(this) : new SkipTransactionsResponseProxyV1(this);
    }

    @Override
    protected SkipTransactionsResponse cloneAsVersion(final ABIVersion version) {
        return new SkipTransactionsResponse(this, version);
    }
}
