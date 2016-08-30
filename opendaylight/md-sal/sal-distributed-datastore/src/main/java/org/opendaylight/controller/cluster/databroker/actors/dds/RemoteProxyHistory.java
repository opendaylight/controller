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

final class RemoteProxyHistory extends AbstractProxyHistory {
    RemoteProxyHistory(final AbstractClientConnection connection, final LocalHistoryIdentifier identifier) {
        super(connection, identifier);
    }

    @Override
    AbstractProxyTransaction doCreateTransactionProxy(final AbstractClientConnection connection,
            final TransactionIdentifier txId) {
        return new RemoteProxyTransaction(connection, txId);
    }
}