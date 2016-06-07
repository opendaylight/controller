/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Externalizable proxy for use with {@link TransactionCanCommitSuccess}. It implements the initial (Boron)
 * serialization format.
 *
 * @author Robert Varga
 */
final class TransactionCanCommitSuccessProxyV1 extends AbstractTransactionSuccessProxy<TransactionCanCommitSuccess> {
    private static final long serialVersionUID = 1L;

    public TransactionCanCommitSuccessProxyV1() {
        // For Externalizable
    }

    TransactionCanCommitSuccessProxyV1(final TransactionCanCommitSuccess success) {
        super(success);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
    }

    @Override
    protected TransactionCanCommitSuccess createSuccess(final TransactionIdentifier target) {
        return new TransactionCanCommitSuccess(target);
    }
}
