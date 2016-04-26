/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.localhistory;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.access.commands.AbstractLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.CompletedTransactionException;
import org.opendaylight.controller.cluster.access.commands.DestroyLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.PendingTransactionException;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalTransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Message;
import org.opendaylight.controller.cluster.datastore.utils.RequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class IdleBehavior extends Behavior {
    private static final Logger LOG = LoggerFactory.getLogger(IdleBehavior.class);

    IdleBehavior(final LocalHistoryContext context) {
        super(context);
    }

    @Override
    final Behavior handleTransactionRequest(final TransactionRequest request) {
        final GlobalTransactionIdentifier txId = request.getIdentifier().getTransactionId();
        final LocalTransactionIdentifier localId = txId.getTransactionId();
        final Long lastCommittedTx = getContext().getLastCommittedTx();
        if (lastCommittedTx != null) {
            final int cmp = Long.compareUnsigned(lastCommittedTx, localId.getTransactionId());
            if (cmp >= 0) {
                // We have committed a previous transaction, hence this one has been successfully completed
                LOG.debug("Transaction has been committed, rejecting request {}", request);
                RequestUtil.sendFailure(request, new CompletedTransactionException(lastCommittedTx));
                return this;
            }
        }

        final RecordedTransaction ctx = getContext().getTransaction(localId.getTransactionId());
        if (ctx != null) {
            // Transaction has been sealed
            final Message<GlobalTransactionIdentifier, ?> msg = ctx.getMessage();
            if (msg == null) {
                // No update yet...
                LOG.debug("Transaction is processing, rejecting request {}", request);
                RequestUtil.sendFailure(request, new PendingTransactionException(ctx.getLastRequest()));
            } else {
                // There has been an update from shard, replay it
                LOG.debug("Transaction is processing, replaying response {} to {}", msg, request);
                request.getReplyTo().tell(msg, ActorRef.noSender());
            }
            return this;
        }

        if (request instanceof AbstractLocalTransactionRequest) {
            return handleLocalTransactionRequest((AbstractLocalTransactionRequest) request);
        } else {
            return createTransaction(request);
        }
    }

    Behavior handleLocalTransactionRequest(final AbstractLocalTransactionRequest request) {
        LOG.debug("Submitting local transaction {}", request);
        getContext().recordTransaction(request);
        return this;
    }

    Behavior createTransaction(final TransactionRequest request) {
        LOG.debug("Opening new transaction for {}", request);
        return new OpenTransactionBehavior(getContext(), request.getIdentifier().getTransactionId())
                .handleTransactionRequest(request);
    }

    @Override
    Behavior handleDestroyLocalHistory(final DestroyLocalHistoryRequest request) {
        LOG.debug("Initiating shutdown on request {}", request);
        return new ClosedBehavior(getContext()).handleDestroyLocalHistory(request);
    }
}
