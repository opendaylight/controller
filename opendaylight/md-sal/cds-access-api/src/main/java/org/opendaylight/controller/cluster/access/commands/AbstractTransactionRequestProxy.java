/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.controller.cluster.access.concepts.AbstractRequestProxy;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Abstract base class for serialization proxies associated with {@link TransactionRequest}s.
 *
 * @param <T> Message type
 */
@Deprecated(since = "7.0.0", forRemoval = true)
abstract class AbstractTransactionRequestProxy<T extends TransactionRequest<T>>
        extends AbstractRequestProxy<TransactionIdentifier, T> implements TransactionRequest.SerialForm<T> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    AbstractTransactionRequestProxy() {
        // For Externalizable
    }

    AbstractTransactionRequestProxy(final T request) {
        super(request);
    }
}
