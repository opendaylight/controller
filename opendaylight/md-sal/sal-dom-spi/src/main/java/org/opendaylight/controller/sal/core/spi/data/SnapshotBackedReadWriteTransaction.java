/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction.TransactionReadyPrototype;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of Read-Write transaction which is backed by {@link DataTreeSnapshot}
 * and executed according to {@link TransactionReadyPrototype}.
 *
 * @param <T> identifier type
 */
@Beta
public final class SnapshotBackedReadWriteTransaction<T> extends SnapshotBackedWriteTransaction<T> implements DOMStoreReadWriteTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(SnapshotBackedReadWriteTransaction.class);

    SnapshotBackedReadWriteTransaction(final T identifier, final boolean debug,
            final DataTreeSnapshot snapshot, final TransactionReadyPrototype<T> readyImpl) {
        super(identifier, debug, snapshot, readyImpl);
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?,?>>, ReadFailedException> read(final YangInstanceIdentifier path) {
        LOG.debug("Tx: {} Read: {}", getIdentifier(), path);
        checkNotNull(path, "Path must not be null.");

        final Optional<NormalizedNode<?, ?>> result;
        try {
            result = readSnapshotNode(path);
        } catch (Exception e) {
            LOG.error("Tx: {} Failed Read of {}", getIdentifier(), path, e);
            return Futures.immediateFailedCheckedFuture(new ReadFailedException("Read failed", e));
        }

        if (result == null) {
            return Futures.immediateFailedCheckedFuture(new ReadFailedException("Transaction is closed"));
        } else {
            return Futures.immediateCheckedFuture(result);
        }
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {
        try {
            return Futures.immediateCheckedFuture(
                read(path).checkedGet().isPresent());
        } catch (ReadFailedException e) {
            return Futures.immediateFailedCheckedFuture(e);
        }
    }
}
