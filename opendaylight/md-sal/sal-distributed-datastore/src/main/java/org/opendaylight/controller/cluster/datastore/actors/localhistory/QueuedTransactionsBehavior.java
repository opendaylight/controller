/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.localhistory;

import java.util.LinkedHashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.access.commands.AbortedTransactionException;
import org.opendaylight.controller.cluster.access.commands.CompletedTransactionException;
import org.opendaylight.controller.cluster.access.commands.PendingTransactionException;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalTransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.RequestUtil;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

final class QueuedTransactionsBehavior extends Behavior {
    private final Map<Long, TransactionContext> queue;
    private Long lastCommittedTx;
    private final long lastKnownTx;

    QueuedTransactionsBehavior(final GlobalTransactionIdentifier gtid, final DataTreeModification tx) {
        queue = new LinkedHashMap<>(2);

        final TransactionContext ctx = new TransactionContext(gtid, tx);
        lastKnownTx = ctx.getTransactionId();
        queue.put(ctx.getTransactionId(), ctx);
    }

    @Override
    Behavior handleTransactionRequest(final TransactionRequest request) {
        final LocalTransactionIdentifier txId = request.getIdentifier().getTransactionId().getTransactionId();
        if (lastCommittedTx != null) {
            final int cmp = Long.compareUnsigned(lastCommittedTx, txId.getTransactionId());
            if (cmp >= 0) {
                // We have committed a previous transaction, hence this one has been successfully completed
                RequestUtil.sendFailure(request, new CompletedTransactionException(lastCommittedTx));
                return this;
            }
        }

        final TransactionContext ctx = queue.get(txId.getTransactionId());
        if (ctx != null) {
            // Transaction has been sealed
            RequestUtil.sendFailure(request, new PendingTransactionException(ctx.lastRequest()));
            return this;
        }

        if (Long.compareUnsigned(lastKnownTx, txId.getTransactionId()) >= 0) {
            // We already seen a newer transaction, which means the previous has not left a mark
            RequestUtil.sendFailure(request, new AbortedTransactionException(lastKnownTx));
            return this;
        }

        return new OpenTransactionBehavior(this).handleTransactionRequest(request);
    }
}