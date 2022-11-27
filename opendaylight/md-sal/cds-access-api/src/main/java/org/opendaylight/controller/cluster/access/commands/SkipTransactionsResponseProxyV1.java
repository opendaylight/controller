/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

/**
 * Externalizable proxy for use with {@link SkipTransactionsResponse}. It implements the initial (Phosphorus SR1)
 * serialization format.
 */
final class SkipTransactionsResponseProxyV1 extends AbstractTransactionSuccessProxy<SkipTransactionsResponse>
        implements SkipTransactionsResponse.SerialForm {
    private static final long serialVersionUID = 1L;

    // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
    // be able to create instances via reflection.
    @SuppressWarnings("checkstyle:RedundantModifier")
    public SkipTransactionsResponseProxyV1() {
        // For Externalizable
    }

    SkipTransactionsResponseProxyV1(final SkipTransactionsResponse success) {
        super(success);
    }
}
