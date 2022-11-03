/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
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
 * Successful reply to an {@link IncrementTransactionSequenceRequest}.
 */
public final class IncrementTransactionSequenceSuccess extends TransactionSuccess<IncrementTransactionSequenceSuccess> {
    @Serial
    private static final long serialVersionUID = 1L;

    public IncrementTransactionSequenceSuccess(final TransactionIdentifier target, final long sequence) {
        super(target, sequence);
    }

    @Override
    protected IncrementTransactionSequenceSuccessProxyV1 externalizableProxy(final ABIVersion version) {
        return new IncrementTransactionSequenceSuccessProxyV1(this);
    }

    @Override
    protected IncrementTransactionSequenceSuccess cloneAsVersion(final ABIVersion version) {
        return this;
    }
}
