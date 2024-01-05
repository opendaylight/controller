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
 * Successful reply to a {@link TransactionPreCommitRequest}.
 *
 * @author Robert Varga
 */
public final class TransactionPreCommitSuccess extends TransactionSuccess<TransactionPreCommitSuccess> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private TransactionPreCommitSuccess(final TransactionPreCommitSuccess success, final ABIVersion version) {
        super(success, version);
    }

    public TransactionPreCommitSuccess(final TransactionIdentifier identifier, final long sequence) {
        super(identifier, sequence);
    }

    @Override
    public TPCS externalizableProxy(final ABIVersion version) {
        return new TPCS(this);
    }

    @Override
    public TransactionPreCommitSuccess cloneAsVersion(final ABIVersion version) {
        return new TransactionPreCommitSuccess(this, version);
    }
}
