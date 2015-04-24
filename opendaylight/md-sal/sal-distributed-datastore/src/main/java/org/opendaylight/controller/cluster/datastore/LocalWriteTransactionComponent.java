/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A local component which is statically bound to a write-only transaction.
 */
final class LocalWriteTransactionComponent extends LocalWritableTransactionComponent<DOMStoreWriteTransaction> {
    private static final Logger LOG = LoggerFactory.getLogger(LocalWriteTransactionComponent.class);

    LocalWriteTransactionComponent(final TransactionIdentifier identifier, final DOMStoreWriteTransaction delegate) {
        super(identifier, delegate);
    }

    @Override
    CheckedFuture<Boolean, ReadFailedException> exists(YangInstanceIdentifier path) {
        throw new UnsupportedOperationException();
    }

    @Override
    CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(YangInstanceIdentifier path) {
        throw new UnsupportedOperationException();
    }

    @Override
    void delete(YangInstanceIdentifier path) {
        LOG.debug("Tx {} delete {}", getIdentifier(), path);
        delegate().delete(path);
    }

    @Override
    void merge(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        LOG.debug("Tx {} merge {}", getIdentifier(), path);
        delegate().merge(path, data);
    }

    @Override
    void write(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        LOG.debug("Tx {} write {}", getIdentifier(), path);
        delegate().write(path, data);
    }
}
