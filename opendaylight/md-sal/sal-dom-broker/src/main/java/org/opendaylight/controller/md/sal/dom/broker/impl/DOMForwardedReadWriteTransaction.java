/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */package org.opendaylight.controller.md.sal.dom.broker.impl;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;

/**
 *
 * Read-Write Transaction, which is composed of several
 * {@link DOMStoreReadWriteTransaction} transactions. Subtransaction is selected by
 * {@link LogicalDatastoreType} type parameter in:
 *
 * <ul>
 * <li>{@link #read(LogicalDatastoreType, InstanceIdentifier)}
 * <li>{@link #put(LogicalDatastoreType, InstanceIdentifier, NormalizedNode)}
 * <li>{@link #delete(LogicalDatastoreType, InstanceIdentifier)}
 * <li>{@link #merge(LogicalDatastoreType, InstanceIdentifier, NormalizedNode)}
 * </ul>
 * {@link #commit()} will result in invocation of
 * {@link DOMDataCommitImplementation#submit(org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction, Iterable)}
 * invocation with all {@link org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort} for underlying
 * transactions.
 *
 */

class DOMForwardedReadWriteTransaction extends DOMForwardedWriteTransaction<DOMStoreReadWriteTransaction> implements
        DOMDataReadWriteTransaction {

    protected DOMForwardedReadWriteTransaction(final Object identifier,
            final ImmutableMap<LogicalDatastoreType, DOMStoreReadWriteTransaction> backingTxs,
            final DOMDataCommitImplementation commitImpl) {
        super(identifier, backingTxs, commitImpl);
    }

    @Override
    public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store,
            final InstanceIdentifier path) {
        return getSubtransaction(store).read(path);
    }
}