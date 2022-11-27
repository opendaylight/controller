/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.annotations.Beta;
import java.io.DataInput;
import java.io.IOException;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Abstract base class for {@link RequestSuccess}es involving specific transaction. This class is visible outside of
 * this package solely for the ability to perform a unified instanceof check.
 *
 * @author Robert Varga
 *
 * @param <T> Message type
 */
@Beta
public abstract class TransactionSuccess<T extends TransactionSuccess<T>>
        extends RequestSuccess<TransactionIdentifier, T> {
    interface SerialForm<T extends TransactionSuccess<T>> extends RequestSuccess.SerialForm<TransactionIdentifier, T> {
        @Override
        default TransactionIdentifier readTarget(final DataInput in) throws IOException {
            return TransactionIdentifier.readFrom(in);
        }
    }

    private static final long serialVersionUID = 1L;

    TransactionSuccess(final TransactionIdentifier identifier, final long sequence) {
        super(identifier, sequence);
    }

    TransactionSuccess(final T success, final ABIVersion version) {
        super(success, version);
    }

    @Override
    protected abstract SerialForm<T> externalizableProxy(ABIVersion version);
}
