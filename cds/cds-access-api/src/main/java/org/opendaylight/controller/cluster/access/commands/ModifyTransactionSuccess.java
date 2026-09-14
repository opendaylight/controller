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
 * Response to a {@link ModifyTransactionRequest} which does not have a {@link PersistenceProtocol}.
 */
public final class ModifyTransactionSuccess extends TransactionSuccess<ModifyTransactionSuccess> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public ModifyTransactionSuccess(final TransactionIdentifier identifier, final long sequence) {
        super(identifier, sequence);
    }

    private ModifyTransactionSuccess(final ModifyTransactionSuccess success, final ABIVersion version) {
        super(success, version);
    }

    @Override
    protected MTS externalizableProxy(final ABIVersion version) {
        return new MTS(this);
    }

    @Override
    protected ModifyTransactionSuccess cloneAsVersion(final ABIVersion version) {
        return new ModifyTransactionSuccess(this, version);
    }
}
