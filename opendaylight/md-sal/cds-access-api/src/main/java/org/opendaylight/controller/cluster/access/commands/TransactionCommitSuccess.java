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
 * Successful reply to a coordinated commit request. It contains a reference to the actor which is handling the commit
 * process.
 *
 * @author Robert Varga
 */
public final class TransactionCommitSuccess extends TransactionSuccess<TransactionCommitSuccess> {
    interface SerialForm extends TransactionSuccess.SerialForm<TransactionCommitSuccess> {
        @Override
        default TransactionCommitSuccess readExternal(final ObjectInput in, final TransactionIdentifier target,
                final long sequence) {
            return new TransactionCommitSuccess(target, sequence);
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private TransactionCommitSuccess(final TransactionCommitSuccess success, final ABIVersion version) {
        super(success, version);
    }

    public TransactionCommitSuccess(final TransactionIdentifier identifier, final long sequence) {
        super(identifier, sequence);
    }

    @Override
    protected SerialForm externalizableProxy(final ABIVersion version) {
        return ABIVersion.MAGNESIUM.lt(version) ? new TCS(this) : new TransactionCommitSuccessProxyV1(this);
    }

    @Override
    protected TransactionCommitSuccess cloneAsVersion(final ABIVersion version) {
        return new TransactionCommitSuccess(this, version);
    }
}
