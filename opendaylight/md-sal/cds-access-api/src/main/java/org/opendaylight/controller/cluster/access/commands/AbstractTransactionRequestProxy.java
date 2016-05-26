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
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestProxy;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Abstract base class for serialization proxies associated with {@link TransactionRequest}s.
 *
 * @author Robert Varga
 *
 * @param <T> Message type
 */
abstract class AbstractTransactionRequestProxy<T extends TransactionRequest<T>> extends AbstractRequestProxy<TransactionIdentifier, T> {
    private static final long serialVersionUID = 1L;

    AbstractTransactionRequestProxy() {
        // For Externalizable
    }

    AbstractTransactionRequestProxy(final T request) {
        super(request);
    }

    @Override
    protected final TransactionIdentifier readTarget(final DataInput in) throws IOException {
        return TransactionIdentifier.readFrom(in);
    }
}
