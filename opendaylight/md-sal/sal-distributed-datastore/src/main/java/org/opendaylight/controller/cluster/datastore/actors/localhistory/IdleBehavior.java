/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.localhistory;

import org.opendaylight.controller.cluster.access.commands.CompletedTransactionException;
import org.opendaylight.controller.cluster.access.commands.PendingTransactionException;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalTransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.RequestUtil;

final class IdleBehavior extends Behavior {
    IdleBehavior(final LocalHistoryContext context) {
        super(context);
    }

    @Override
    Behavior handleTransactionRequest(final TransactionRequest request) {
        final GlobalTransactionIdentifier txId = request.getIdentifier().getTransactionId();
        final LocalTransactionIdentifier localId = txId.getTransactionId();
        final Long lastCommittedTx = getContext().getLastCommittedTx();
        if (lastCommittedTx != null) {
            final int cmp = Long.compareUnsigned(lastCommittedTx, localId.getTransactionId());
            if (cmp >= 0) {
                // We have committed a previous transaction, hence this one has been successfully completed
                RequestUtil.sendFailure(request, new CompletedTransactionException(lastCommittedTx));
                return this;
            }
        }

        final TransactionContext ctx = getContext().getTransaction(localId.getTransactionId());
        if (ctx != null) {
            // Transaction has been sealed
            RequestUtil.sendFailure(request, new PendingTransactionException(ctx.lastRequest()));
            return this;
        }

        return new OpenTransactionBehavior(getContext(), txId).handleTransactionRequest(request);
    }
}