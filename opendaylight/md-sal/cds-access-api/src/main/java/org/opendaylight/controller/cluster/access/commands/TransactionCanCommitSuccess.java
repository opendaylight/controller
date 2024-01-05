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
 */
public final class TransactionCanCommitSuccess extends TransactionSuccess<TransactionCanCommitSuccess> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private TransactionCanCommitSuccess(final TransactionCanCommitSuccess success, final ABIVersion version) {
        super(success, version);
    }

    public TransactionCanCommitSuccess(final TransactionIdentifier identifier, final long sequence) {
        super(identifier, sequence);
    }

    @Override
    public TCCS externalizableProxy(final ABIVersion version) {
        return new TCCS(this);
    }

    @Override
    public TransactionCanCommitSuccess cloneAsVersion(final ABIVersion version) {
        return new TransactionCanCommitSuccess(this, version);
    }
}
