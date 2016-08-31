/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import java.util.Optional;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AbstractClientHistory} which handles free-standing transactions.
 *
 * @author Robert Varga
 */
final class SingleClientHistory extends AbstractClientHistory {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractClientHistory.class);

    SingleClientHistory(final DistributedDataStoreClientBehavior client, final LocalHistoryIdentifier identifier) {
        super(client, identifier);
    }

    @Override
    ClientTransaction doCreateTransaction() {
        final TransactionIdentifier txId = new TransactionIdentifier(getIdentifier(), nextTx());
        LOG.debug("{}: creating a new transaction {}", this, txId);

        return new ClientTransaction(this, txId);
    }

    @Override
    AbstractProxyHistory createHistoryProxy(final LocalHistoryIdentifier historyId,
            final Optional<ShardBackendInfo> backendInfo) {
        return AbstractProxyHistory.createSingle(getClient(), backendInfo, historyId);
    }
}
