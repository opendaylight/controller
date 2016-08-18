/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

final class LocalProxyHistory extends AbstractProxyHistory {
    private final DataTree dataTree;

    LocalProxyHistory(DistributedDataStoreClientBehavior client, LocalHistoryIdentifier identifier, DataTree dataTree) {
        super(client, identifier);
        this.dataTree = Preconditions.checkNotNull(dataTree);
    }

    @Override
    AbstractProxyTransaction doCreateTransactionProxy(final DistributedDataStoreClientBehavior client,
            final TransactionIdentifier txId) {
        // FIXME: this violates history contract: we should use the last submitted transaction instead to ensure
        //        causality
        return new LocalProxyTransaction(client, txId, dataTree.takeSnapshot());
    }
}