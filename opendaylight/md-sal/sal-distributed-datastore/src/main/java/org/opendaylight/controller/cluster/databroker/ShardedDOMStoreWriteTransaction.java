/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Proxy implementation of {@link DOMStoreWriteTransaction}. It routes all requests to the backing
 * {@link ClientTransaction}.
 *
 * @author Robert Varga
 */
final class ShardedDOMStoreWriteTransaction extends AbstractShardedTransaction implements DOMStoreWriteTransaction {
    ShardedDOMStoreWriteTransaction(final ClientTransaction tx) {
        super(tx);
    }

    @Override
    public void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        transaction().write(path, data);
    }

    @Override
    public void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        transaction().merge(path, data);
    }

    @Override
    public void delete(final YangInstanceIdentifier path) {
        transaction().delete(path);
    }

    @Override
    public DOMStoreThreePhaseCommitCohort ready() {
        return transaction().ready();
    }

    @Override
    public final void close() {
        transaction().abort();
    }
}
