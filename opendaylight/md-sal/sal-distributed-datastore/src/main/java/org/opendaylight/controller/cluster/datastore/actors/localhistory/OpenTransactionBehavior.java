/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.localhistory;

import akka.actor.ActorRef;
import com.google.common.base.Optional;
import org.opendaylight.controller.cluster.access.commands.BusyLocalHistoryException;
import org.opendaylight.controller.cluster.access.commands.DestroyLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.OutOfOrderRequestException;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionModification;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.RequestUtil;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class OpenTransactionBehavior extends Behavior {
    private static final Logger LOG = LoggerFactory.getLogger(OpenTransactionBehavior.class);
    private final TransactionContext tx;
    private long expectedRequest = 0;

    OpenTransactionBehavior(final LocalHistoryContext context, final GlobalTransactionIdentifier transactionId) {
        super(context);
        this.tx = context.allocateTransaction(transactionId);
    }

    @Override
    Behavior handleTransactionRequest(final TransactionRequest request) {
        // We are about to initiate a new transaction, check if this is the first request
        if (request.getIdentifier().getRequestId() != expectedRequest) {
            // This is not the first request for the new transaction, reject it
            RequestUtil.sendFailure(request, new OutOfOrderRequestException(expectedRequest));
            return this;
        }

        if (request instanceof ReadTransactionRequest) {
            return handleReadRequest((ReadTransactionRequest) request);
        } else if (request instanceof ModifyTransactionRequest) {
            return handleModifyRequest((ModifyTransactionRequest) request);
        } else {
            throw new IllegalArgumentException("Unhandled request " + request);
        }
    }

    private void updateExpectedRequest(final TransactionRequest request) {
        expectedRequest = request.getIdentifier().getRequestId() + 1;
    }

    private Behavior handleReadRequest(final ReadTransactionRequest request) {
        final Optional<NormalizedNode<?, ?>> node = tx.read(request.getPath());
        request.getReplyTo().tell(new ReadTransactionSuccess(request.getIdentifier(), node), ActorRef.noSender());
        updateExpectedRequest(request);
        return this;
    }

    private Behavior handleModifyRequest(final ModifyTransactionRequest request) {
        for (TransactionModification op : request.getModifications()) {
            try {
                tx.applyOperation(op);
            } catch (Exception e) {
                LOG.warn("Failed to apply operation {}", op, e);
                // FIXME: report the error back to caller and mark this transaction as aborted
                break;
            }
        }

        updateExpectedRequest(request);

        switch (request.getFinish()) {
            case ABORT:
                getContext().recordTransaction(tx, TransactionContext.Fate.ABORTED);
                break;
            case SIMPLE_COMMIT:
                getContext().recordTransaction(tx, TransactionContext.Fate.SIMPLE_COMMIT);
                break;
            case COORDINATED_COMMIT:
                getContext().recordTransaction(tx, TransactionContext.Fate.COORDINATED_COMMIT);
                break;
            case NONE:
                return this;
            default:
                LOG.error("Unhandled finish type {}, assuming NONE", request.getFinish());
                return this;
        }

        return new IdleBehavior(getContext());
    }

    @Override
    Behavior handleDestroyLocalHistory(final DestroyLocalHistoryRequest request) {
        RequestUtil.sendFailure(request, new BusyLocalHistoryException(tx.getTransactionId()));
        return this;
    }
}