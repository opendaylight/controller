/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import java.io.ObjectInput;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Successful reply to a coordinated commit request initiated by a {@link ModifyTransactionRequest}
 * or {@link CommitLocalTransactionRequest}.
 *
 * @author Robert Varga
 */
public final class TransactionCanCommitSuccess extends TransactionSuccess<TransactionCanCommitSuccess> {
    interface SerialForm extends TransactionSuccess.SerialForm<TransactionCanCommitSuccess> {
        @Override
        default TransactionCanCommitSuccess readExternal(final ObjectInput in, final TransactionIdentifier target,
                final long sequence) {
            return new TransactionCanCommitSuccess(target, sequence);
        }
    }

    private static final long serialVersionUID = 1L;

    private TransactionCanCommitSuccess(final TransactionCanCommitSuccess success, final ABIVersion version) {
        super(success, version);
    }

    public TransactionCanCommitSuccess(final TransactionIdentifier identifier, final long sequence) {
        super(identifier, sequence);
    }

    @Override
    protected SerialForm externalizableProxy(final ABIVersion version) {
        return ABIVersion.MAGNESIUM.lt(version) ? new TCCS(this) : new TransactionCanCommitSuccessProxyV1(this);
    }

    @Override
    protected TransactionCanCommitSuccess cloneAsVersion(final ABIVersion version) {
        return new TransactionCanCommitSuccess(this, version);
    }
}
