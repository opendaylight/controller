/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.databroker.mdsal;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Read transaction that delegates calls to {@link org.opendaylight.mdsal.dom.broker.ShardedDOMReadTransactionAdapter},
 * which in turn translates calls to shard aware implementation of {@link org.opendaylight.mdsal.dom.api.DOMDataTreeService}.
 * <p>
 * Since reading data distributed on different subshards is not guaranteed to
 * return all relevant data, we cannot guarantee it neither. Best effort is to
 * return all data we get from first initial data change event received.
 */
class MdsalDelegatingReadTransaction implements DOMDataReadOnlyTransaction {
    private final DOMDataTreeReadTransaction delegateTx;
    private final Object txIdentifier;

    public MdsalDelegatingReadTransaction(final Object txIdentifier, final DOMDataTreeReadTransaction delegateTx) {
        this.delegateTx = checkNotNull(delegateTx);
        this.txIdentifier = checkNotNull(txIdentifier);
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final LogicalDatastoreType store,
                                                                                   final YangInstanceIdentifier path) {
        return Futures.makeChecked(delegateTx.read(MdsalDelegatingDataBrokerUtils.translateDataStoreType(store), path), ReadFailedException.MAPPER);
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        return Futures.makeChecked(delegateTx.exists(MdsalDelegatingDataBrokerUtils.translateDataStoreType(store), path), ReadFailedException.MAPPER);
    }

    @Override
    public Object getIdentifier() {
        return txIdentifier;
    }

    @Override
    public void close() {
        delegateTx.close();
    }
}
