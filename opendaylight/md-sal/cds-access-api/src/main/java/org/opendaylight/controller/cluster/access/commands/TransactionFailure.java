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
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Generic {@link RequestFailure} involving a {@link TransactionRequest}.
 */
public final class TransactionFailure extends RequestFailure<TransactionIdentifier, TransactionFailure> {
    interface SerialForm extends RequestFailure.SerialForm<TransactionIdentifier, TransactionFailure> {
        @Override
        default TransactionIdentifier readTarget(final DataInput in) throws IOException {
            return TransactionIdentifier.readFrom(in);
        }

        @Override
        default TransactionFailure createFailure(final TransactionIdentifier target, final long sequence,
                final RequestException cause) {
            return new TransactionFailure(target, sequence, cause);
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private TransactionFailure(final TransactionFailure failure, final ABIVersion version) {
        super(failure, version);
    }

    TransactionFailure(final TransactionIdentifier target, final long sequence, final RequestException cause) {
        super(target, sequence, cause);
    }

    @Override
    protected TransactionFailure cloneAsVersion(final ABIVersion version) {
        return new TransactionFailure(this, version);
    }

    @Override
    protected SerialForm externalizableProxy(final ABIVersion version) {
        return new TF(this);
    }
}
