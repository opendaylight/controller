/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

final class IncrementTransactionSequenceRequestProxyV1
        extends AbstractReadTransactionRequestProxyV1<IncrementTransactionSequenceRequest>
        implements IncrementTransactionSequenceRequest.SerialForm {
    private static final long serialVersionUID = -7345885599575376005L;

    // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
    // be able to create instances via reflection.
    @SuppressWarnings("checkstyle:RedundantModifier")
    public IncrementTransactionSequenceRequestProxyV1() {
        // For Externalizable
    }

    IncrementTransactionSequenceRequestProxyV1(final IncrementTransactionSequenceRequest request) {
        super(request);
    }
}
