/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.legacy.sharded.adapter;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.util.concurrent.FluentFuture;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

class ShardedDOMDataBrokerDelegatingWriteTransaction implements DOMDataWriteTransaction {
    private final DOMDataTreeWriteTransaction delegateTx;
    private final Object txIdentifier;

    ShardedDOMDataBrokerDelegatingWriteTransaction(final Object txIdentifier,
                                                          final DOMDataTreeWriteTransaction delegateTx) {
        this.delegateTx = checkNotNull(delegateTx);
        this.txIdentifier = checkNotNull(txIdentifier);
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                    final NormalizedNode<?, ?> data) {
        delegateTx.put(store.toMdsal(), path, data);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                      final NormalizedNode<?, ?> data) {
        delegateTx.merge(store.toMdsal(), path, data);
    }

    @Override
    public boolean cancel() {
        return delegateTx.cancel();
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        delegateTx.delete(store.toMdsal(), path);
    }

    @Override
    public FluentFuture<? extends CommitInfo> commit() {
        return delegateTx.commit();
    }

    @Override
    public Object getIdentifier() {
        return txIdentifier;
    }
}
