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
 * Externalizable proxy for use with {@link TransactionCommitSuccess}. It implements the initial (Boron)
 * serialization format.
 *
 * @author Robert Varga
 */
final class TransactionCommitSuccessProxyV1 extends AbstractTransactionSuccessProxy<TransactionCommitSuccess> {
    private static final long serialVersionUID = 1L;

    public TransactionCommitSuccessProxyV1() {
        // For Externalizable
    }

    TransactionCommitSuccessProxyV1(final TransactionCommitSuccess success) {
        super(success);
    }

    @Override
    protected TransactionCommitSuccess createSuccess(final TransactionIdentifier target) {
        return new TransactionCommitSuccess(target);
    }
}
