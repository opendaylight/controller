/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Externalizable proxy for use with {@link TransactionPreCommitSuccess}. It implements the initial (Boron)
 * serialization format.
 *
 * @author Robert Varga
 */
final class TransactionPreCommitSuccessProxyV1 extends AbstractTransactionSuccessProxy<TransactionPreCommitSuccess> {
    private static final long serialVersionUID = 1L;

    public TransactionPreCommitSuccessProxyV1() {
        // For Externalizable
    }

    TransactionPreCommitSuccessProxyV1(final TransactionPreCommitSuccess success) {
        super(success);
    }

    @Override
    protected TransactionPreCommitSuccess createSuccess(final TransactionIdentifier target, final long sequence) {
        return new TransactionPreCommitSuccess(target, sequence);
    }
}
