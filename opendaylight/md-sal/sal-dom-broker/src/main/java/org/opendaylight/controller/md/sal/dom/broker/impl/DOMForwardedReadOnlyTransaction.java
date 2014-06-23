/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;

/**
 *
 * Read Only Transaction, which is composed of several
 * {@link DOMStoreReadTransaction} transactions. Subtransaction is selected by
 * {@link LogicalDatastoreType} type parameter in
 * {@link #read(LogicalDatastoreType, InstanceIdentifier)}.
 */
class DOMForwardedReadOnlyTransaction extends
        AbstractDOMForwardedCompositeTransaction<LogicalDatastoreType, DOMStoreReadTransaction> implements
        DOMDataReadOnlyTransaction {

    protected DOMForwardedReadOnlyTransaction(final Object identifier,
            final ImmutableMap<LogicalDatastoreType, DOMStoreReadTransaction> backingTxs) {
        super(identifier, backingTxs);
    }

    @Override
    public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store,
            final InstanceIdentifier path) {
        return getSubtransaction(store).read(path);
    }

    @Override
    public void close() {
        closeSubtransactions();
    }

}
