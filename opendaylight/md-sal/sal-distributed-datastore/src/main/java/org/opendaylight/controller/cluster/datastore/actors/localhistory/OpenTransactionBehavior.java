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
import org.opendaylight.controller.cluster.access.commands.AbstractLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.AbstractReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.BusyLocalHistoryException;
import org.opendaylight.controller.cluster.access.commands.DestroyLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.ModificationFailedException;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.OutOfOrderRequestException;
import org.opendaylight.controller.cluster.access.commands.PersistenceProtocol;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionModification;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.RequestUtil;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class OpenTransactionBehavior extends Behavior {
    private static final Logger LOG = LoggerFactory.getLogger(OpenTransactionBehavior.class);
    private final TransactionContext tx;

    OpenTransactionBehavior(final LocalHistoryContext context, final GlobalTransactionIdentifier transactionId) {
        super(context);
        this.tx = context.allocateTransaction(transactionId);
    }

    @Override
    Behavior handleTransactionRequest(final TransactionRequest request) {
        // We are about to initiate a new transaction, check if this is the first request
        if (request.getIdentifier().getRequestId() != tx.getExpectedRequest()) {
            // This is not the first request for the new transaction, reject it
            RequestUtil.sendFailure(request, new OutOfOrderRequestException(tx.getExpectedRequest()));
            return this;
        }

        if (request instanceof AbstractReadTransactionRequest) {
            return handleReadRequest((AbstractReadTransactionRequest) request);
        } else if (request instanceof ModifyTransactionRequest) {
            return handleModifyRequest((ModifyTransactionRequest) request);
        } else if (request instanceof AbstractLocalTransactionRequest) {
            RequestUtil.sendFailure(request, new BusyLocalHistoryException(tx.getTransactionId()));
            return this;
        } else {
            throw new IllegalArgumentException("Unhandled request " + request);
        }
    }

    private void updateExpectedRequest(final TransactionRequest request) {
        tx.setExpectedRequest(request.getIdentifier().getRequestId() + 1);
    }

    private Behavior handleReadRequest(final AbstractReadTransactionRequest request) {
        final Optional<NormalizedNode<?, ?>> node = tx.read(request.getPath());
        LOG.trace("Read request {} resulted in {}", request, node);

        final TransactionSuccess reply;
        if (request instanceof ReadTransactionRequest) {
            reply = new ReadTransactionSuccess(request.getIdentifier(), node);
        } else if (request instanceof ExistsTransactionRequest) {
            reply = new ExistsTransactionSuccess(request.getIdentifier(), node.isPresent());
        } else {
            throw new IllegalArgumentException("Unhandled read request " + request);
        }

        request.getReplyTo().tell(reply, ActorRef.noSender());
        updateExpectedRequest(request);
        return this;
    }

    private Behavior handleModifyRequest(final ModifyTransactionRequest request) {
        updateExpectedRequest(request);

        for (TransactionModification op : request.getModifications()) {
            try {
                LOG.trace("Applying operation {}", op);
                tx.applyOperation(op);
            } catch (Exception e) {
                LOG.info("Failed to apply operation {}", op, e);
                RequestUtil.sendFailure(request, new ModificationFailedException(e));
                getContext().abortTransaction(tx, e);
                return new IdleBehavior(getContext());
            }
        }

        final java.util.Optional<PersistenceProtocol> maybeProtocol = request.getPersistenceProtocol();
        if (!maybeProtocol.isPresent()) {
            return this;
        }

        final PersistenceProtocol protocol = maybeProtocol.get();
        switch (protocol) {
            case ABORT:
                getContext().abortTransaction(tx);
                break;
            case SIMPLE:
                getContext().doCommitTransaction(tx);
                break;
            case THREE_PHASE:
                getContext().canCommitTransaction(tx);
                break;
            default:
                LOG.error("Unhandled persistence protocol {}, assuming ABORT", request.getPersistenceProtocol());
                getContext().abortTransaction(tx);
        }

        return new IdleBehavior(getContext());
    }

    @Override
    Behavior handleDestroyLocalHistory(final DestroyLocalHistoryRequest request) {
        LOG.warn("Attempted to close history {} while transaction {} is open", historyId(), tx.getTransactionId());
        RequestUtil.sendFailure(request, new BusyLocalHistoryException(tx.getTransactionId()));
        return this;
    }
}
