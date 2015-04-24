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
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * A local component which is statically bound to a read-write transaction.
 */
final class LocalReadWriteTransactionComponent extends LocalWritableTransactionComponent<DOMStoreReadWriteTransaction> {
    LocalReadWriteTransactionComponent(final DOMStoreReadWriteTransaction delegate) {
        super(delegate);
    }

    @Override
    CheckedFuture<Boolean, ReadFailedException> exists(YangInstanceIdentifier path) {
        return delegate().exists(path);
    }

    @Override
    CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(YangInstanceIdentifier path) {
        return delegate().read(path);
    }

    @Override
    void delete(YangInstanceIdentifier path) {
        delegate().delete(path);
    }

    @Override
    void merge(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        delegate().merge(path, data);
    }

    @Override
    void write(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        delegate().write(path, data);
    }
}
