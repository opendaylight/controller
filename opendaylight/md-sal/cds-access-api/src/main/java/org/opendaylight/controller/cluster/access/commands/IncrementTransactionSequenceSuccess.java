/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Successful reply to an {@link IncrementTransactionSequenceRequest}.
 */
public final class IncrementTransactionSequenceSuccess extends TransactionSuccess<IncrementTransactionSequenceSuccess> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private IncrementTransactionSequenceSuccess(final IncrementTransactionSequenceSuccess success,
            final ABIVersion version) {
        super(success, version);
    }

    public IncrementTransactionSequenceSuccess(final TransactionIdentifier target, final long sequence) {
        super(target, sequence);
    }

    @Override
    protected ITSS externalizableProxy(final ABIVersion version) {
        return new ITSS(this);
    }

    @Override
    protected IncrementTransactionSequenceSuccess cloneAsVersion(final ABIVersion version) {
        return new IncrementTransactionSequenceSuccess(this, version);
    }
}
