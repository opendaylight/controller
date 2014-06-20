/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Implementation of Read-Write transaction which is backed by {@link DataTreeSnapshot}
 * and executed according to {@link TransactionReadyPrototype}.
 *
 */
class SnapshotBackedReadWriteTransaction extends SnapshotBackedWriteTransaction implements
DOMStoreReadWriteTransaction {

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
    public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final InstanceIdentifier path) {
        LOG.debug("Tx: {} Read: {}", getIdentifier(), path);
        try {
            return Futures.immediateFuture(getMutatedView().readNode(path));
        } catch (Exception e) {
            LOG.error("Tx: {} Failed Read of {}", getIdentifier(), path, e);
            throw e;
        }
    }
}