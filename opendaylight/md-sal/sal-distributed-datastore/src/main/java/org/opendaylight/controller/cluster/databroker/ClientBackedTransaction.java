/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.sal.core.spi.data.AbstractDOMStoreTransaction;

/**
 * An implementation of {@link DOMStoreTransaction} backed by a {@link ClientTransaction}.
 *
 * @author Robert Varga
 */
abstract class ClientBackedTransaction extends AbstractDOMStoreTransaction<TransactionIdentifier> {
    private final ClientTransaction delegate;

    ClientBackedTransaction(final ClientTransaction delegate) {
        super(delegate.getIdentifier());
        this.delegate = delegate;
    }

    final ClientTransaction delegate() {
        return delegate;
    }

    @Override
    public final void close() {
        delegate.abort();
    }
}
