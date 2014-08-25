/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 * Implementation of read-only transaction backed by {@link DataTreeSnapshot}
 *
 * Implementation of read-only transaction backed by {@link DataTreeSnapshot}
 * which delegates most of its calls to similar methods provided by underlying snapshot.
 *
 */
final class SnapshotBackedReadTransaction extends AbstractDOMStoreTransaction
                                          implements DOMStoreReadTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(SnapshotBackedReadTransaction.class);
    private volatile DataTreeSnapshot stableSnapshot;

    public SnapshotBackedReadTransaction(final Object identifier, final DataTreeSnapshot snapshot) {
        super(identifier);
        this.stableSnapshot = Preconditions.checkNotNull(snapshot);
        LOG.debug("ReadOnly Tx: {} allocated with snapshot {}", identifier, snapshot);
    }

    @Override
    public void close() {
        LOG.debug("Store transaction: {} : Closed", getIdentifier());
        stableSnapshot = null;
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?,?>>, ReadFailedException> read(final YangInstanceIdentifier path) {
        LOG.debug("Tx: {} Read: {}", getIdentifier(), path);
        checkNotNull(path, "Path must not be null.");

        final DataTreeSnapshot snapshot = stableSnapshot;
        if (snapshot == null) {
            return Futures.immediateFailedCheckedFuture(new ReadFailedException("Transaction is closed"));
        }

        try {
            return Futures.immediateCheckedFuture(snapshot.readNode(path));
        } catch (Exception e) {
            LOG.error("Tx: {} Failed Read of {}", getIdentifier(), path, e);
            return Futures.immediateFailedCheckedFuture(new ReadFailedException("Read failed",e));
        }
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(YangInstanceIdentifier path) {
        LOG.debug("Tx: {} Exists: {}", getIdentifier(), path);
        checkNotNull(path, "Path must not be null.");

        try {
            return Futures.immediateCheckedFuture(
                read(path).checkedGet().isPresent());
        } catch (ReadFailedException e) {
            return Futures.immediateFailedCheckedFuture(e);
        }
    }
}
