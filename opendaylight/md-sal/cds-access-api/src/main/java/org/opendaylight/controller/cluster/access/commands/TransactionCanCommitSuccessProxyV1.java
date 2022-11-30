/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

/**
 * Externalizable proxy for use with {@link TransactionCanCommitSuccess}. It implements the initial (Boron)
 * serialization format.
 */
@Deprecated(since = "7.0.0", forRemoval = true)
final class TransactionCanCommitSuccessProxyV1 extends AbstractTransactionSuccessProxy<TransactionCanCommitSuccess>
        implements TransactionCanCommitSuccess.SerialForm {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
    // be able to create instances via reflection.
    @SuppressWarnings("checkstyle:RedundantModifier")
    public TransactionCanCommitSuccessProxyV1() {
        // For Externalizable
    }

    TransactionCanCommitSuccessProxyV1(final TransactionCanCommitSuccess success) {
        super(success);
    }
}
