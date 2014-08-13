/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;

/**
 * Implementation of Read-Write transaction which is backed by {@link DataTreeSnapshot}
 * and executed according to {@link TransactionReadyPrototype}.
 *
 */
class SnapshotBackedReadWriteTransaction extends SnapshotBackedWriteTransaction
                                         implements DOMStoreReadWriteTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(SnapshotBackedReadWriteTransaction.class);

    /**
     * Creates new read-write transaction.
     *
     * @param identifier transaction Identifier
     * @param snapshot Snapshot which will be modified.
     * @param readyImpl Implementation of ready method.
     */
    protected SnapshotBackedReadWriteTransaction(final Object identifier, final DataTreeSnapshot snapshot,
            final TransactionReadyPrototype store) {
        super(identifier, snapshot, store);
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?,?>>, ReadFailedException> read(final YangInstanceIdentifier path) {
        LOG.debug("Tx: {} Read: {}", getIdentifier(), path);
        checkNotNull(path, "Path must not be null.");

        DataTreeModification dataView = getMutatedView();
        if(dataView == null) {
            return Futures.immediateFailedCheckedFuture(new ReadFailedException("Transaction is closed"));
        }

        try {
            return Futures.immediateCheckedFuture(dataView.readNode(path));
        } catch (Exception e) {
            LOG.error("Tx: {} Failed Read of {}", getIdentifier(), path, e);
            return Futures.immediateFailedCheckedFuture(new ReadFailedException("Read failed",e));
        }
    }

    @Override public CheckedFuture<Boolean, ReadFailedException> exists(
        YangInstanceIdentifier path) {
        try {
            return Futures.immediateCheckedFuture(
                read(path).checkedGet().isPresent());
        } catch (ReadFailedException e) {
            return Futures.immediateFailedCheckedFuture(e);
        }
    }
}
