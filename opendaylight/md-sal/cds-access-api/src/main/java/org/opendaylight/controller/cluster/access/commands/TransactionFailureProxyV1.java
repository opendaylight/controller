/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import java.io.DataInput;
import java.io.IOException;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestFailureProxy;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Externalizable proxy for use with {@link TransactionFailure}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class TransactionFailureProxyV1 extends AbstractRequestFailureProxy<TransactionIdentifier, TransactionFailure> {
    private static final long serialVersionUID = 1L;

    public TransactionFailureProxyV1() {
        // For Externalizable
    }

    TransactionFailureProxyV1(final TransactionFailure failure) {
        super(failure);
    }

    @Override
    protected TransactionFailure createFailure(final TransactionIdentifier target, final long sequence,
            final RequestException cause) {
        return new TransactionFailure(target, sequence, cause);
    }

    @Override
    protected TransactionIdentifier readTarget(final DataInput in) throws IOException {
        return TransactionIdentifier.readFrom(in);
    }
}
