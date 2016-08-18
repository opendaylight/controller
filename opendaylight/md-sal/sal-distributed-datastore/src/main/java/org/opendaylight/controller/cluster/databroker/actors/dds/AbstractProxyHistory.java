/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.util.Optional;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

/**
 * Per-connection representation of a local history.
 *
 * @author Robert Varga
 */
abstract class AbstractProxyHistory implements Identifiable<LocalHistoryIdentifier> {
    // FIXME: this should really be ClientConnection
    private final DistributedDataStoreClientBehavior client;
    private final LocalHistoryIdentifier identifier;

    AbstractProxyHistory(final DistributedDataStoreClientBehavior client, final LocalHistoryIdentifier identifier) {
        this.client = Preconditions.checkNotNull(client);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    static AbstractProxyHistory create(final DistributedDataStoreClientBehavior client,
            final Optional<ShardBackendInfo> backendInfo, final LocalHistoryIdentifier identifier) {
        final Optional<DataTree> dataTree = backendInfo.flatMap(ShardBackendInfo::getDataTree);
        return dataTree.isPresent() ? new LocalProxyHistory(client, identifier, dataTree.get()) : new RemoteProxyHistory(client, identifier);
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    final ActorRef localActor() {
        return client.self();
    }

    final AbstractProxyTransaction createTransactionProxy(final TransactionIdentifier txId) {
        return doCreateTransactionProxy(client, new TransactionIdentifier(identifier, txId.getTransactionId()));
    }

    abstract AbstractProxyTransaction doCreateTransactionProxy(DistributedDataStoreClientBehavior client,
            TransactionIdentifier txId);
}
