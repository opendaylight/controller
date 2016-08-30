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
 * Per-connection representation of a local history. This class handles state replication across a single connection.
 *
 * @author Robert Varga
 */
abstract class AbstractProxyHistory implements Identifiable<LocalHistoryIdentifier> {
    private final AbstractClientConnection connection;
    private final LocalHistoryIdentifier identifier;

    AbstractProxyHistory(final AbstractClientConnection connection, final LocalHistoryIdentifier identifier) {
        this.connection = Preconditions.checkNotNull(connection);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    static AbstractProxyHistory createClient(final AbstractClientConnection connection,
            final LocalHistoryIdentifier identifier) {
        final Optional<DataTree> dataTree = connection.getBackendInfo().flatMap(ShardBackendInfo::getDataTree);
        return dataTree.isPresent() ? new ClientLocalProxyHistory(connection, identifier, dataTree.get())
             : new RemoteProxyHistory(connection, identifier);
    }

    static AbstractProxyHistory createSingle(final AbstractClientConnection connection,
            final LocalHistoryIdentifier identifier) {
        final Optional<DataTree> dataTree = connection.getBackendInfo().flatMap(ShardBackendInfo::getDataTree);
        return dataTree.isPresent() ? new SingleLocalProxyHistory(connection, identifier, dataTree.get())
             : new RemoteProxyHistory(connection, identifier);
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    final ActorRef localActor() {
        return connection.localActor();
    }

    final AbstractProxyTransaction createTransactionProxy(final TransactionIdentifier txId) {
        return doCreateTransactionProxy(connection, new TransactionIdentifier(identifier, txId.getTransactionId()));
    }

    abstract AbstractProxyTransaction doCreateTransactionProxy(AbstractClientConnection connection,
            TransactionIdentifier txId);
}
