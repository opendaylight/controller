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
 * Successful reply to a coordinated commit request initiated by a {@link ModifyTransactionRequest}
 * or {@link CommitLocalTransactionRequest}.
 *
 * @author Robert Varga
 */
public final class TransactionAbortSuccess extends TransactionSuccess<TransactionAbortSuccess> {
    private static final long serialVersionUID = 1L;

    public TransactionAbortSuccess(final TransactionIdentifier identifier, final long sequence) {
        super(identifier, sequence);
    }

    @Override
    protected AbstractTransactionSuccessProxy<TransactionAbortSuccess> externalizableProxy(final ABIVersion version) {
        return new TransactionAbortSuccessProxyV1(this);
    }

    @Override
    protected TransactionAbortSuccess cloneAsVersion(final ABIVersion version) {
        return this;
    }
}
