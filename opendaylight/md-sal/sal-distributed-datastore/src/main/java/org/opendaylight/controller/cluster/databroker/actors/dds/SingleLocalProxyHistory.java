/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

final class SingleLocalProxyHistory extends AbstractLocalProxyHistory {
    SingleLocalProxyHistory(final DistributedDataStoreClientBehavior client, final LocalHistoryIdentifier identifier,
        final DataTree dataTree) {
        super(client, identifier, dataTree);
    }

    @Override
    AbstractProxyTransaction doCreateTransactionProxy(final DistributedDataStoreClientBehavior client,
            final TransactionIdentifier txId) {
        return new LocalProxyTransaction(client, txId, takeSnapshot());
    }
}