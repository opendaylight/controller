/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

/**
 * Externalizable proxy for use with {@link SkipTransactionsRequest}. It implements the initial
 * (Phosphorus SR1) serialization format.
 */
final class SkipTransactionsRequestV1 extends AbstractTransactionRequestProxy<SkipTransactionsRequest>
        implements SkipTransactionsRequest.SerialForm {
    @java.io.Serial
    private static final long serialVersionUID = -7493419007644462643L;

    // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
    // be able to create instances via reflection.
    @SuppressWarnings("checkstyle:RedundantModifier")
    public SkipTransactionsRequestV1() {
        // For Externalizable
    }

    SkipTransactionsRequestV1(final SkipTransactionsRequest request) {
        super(request);
    }
}
