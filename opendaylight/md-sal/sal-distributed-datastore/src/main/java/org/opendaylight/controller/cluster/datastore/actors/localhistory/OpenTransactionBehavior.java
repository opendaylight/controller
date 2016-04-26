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
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.OutOfOrderRequestException;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionDelete;
import org.opendaylight.controller.cluster.access.commands.TransactionMerge;
import org.opendaylight.controller.cluster.access.commands.TransactionModification;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionWrite;
import org.opendaylight.controller.cluster.datastore.utils.RequestUtil;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

final class OpenTransactionBehavior extends Behavior {
    private final Behavior previousBehavior;
    private final DataTreeModification tx;
    private long expectedRequest;

    OpenTransactionBehavior(final Behavior previousBehavior) {
        this.previousBehavior = Preconditions.checkNotNull(previousBehavior);
        // FIXME: need a DataTreeTip
        tx = null;
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
        final Optional<NormalizedNode<?, ?>> node = tx.readNode(request.getPath());
        request.getReplyTo().tell(new ReadTransactionSuccess(request.getIdentifier(), node), ActorRef.noSender());
        updateExpectedRequest(request);
        return this;
    }

    private Behavior handleModifyRequest(final ModifyTransactionRequest request) {

        for (TransactionModification op : request.getModifications()) {
            // FIXME: handle errors thrown by the operation
            if (op instanceof TransactionDelete) {
                tx.delete(op.getPath());
            } else if (op instanceof TransactionMerge) {
                tx.merge(op.getPath(), ((TransactionMerge) op).getData());
            } else if (op instanceof TransactionWrite) {
                tx.write(op.getPath(), ((TransactionWrite) op).getData());
            } else {
                throw new IllegalArgumentException("Unhandled request " + request);
            }
        }

        updateExpectedRequest(request);

        switch (request.getFinish()) {
            case NONE:
                return this;
            case ABORT:
                previousBehavior.recordAbortedTransaction(request.getIdentifier().getTransactionId());
                request.getReplyTo().tell(null, ActorRef.noSender());
                return previousBehavior;
            case SIMPLE_COMMIT:
                // FIXME: return as appropriate
                throw new UnsupportedOperationException();
            case COORDINATED_COMMIT:
                // FIXME: return as appropriate
                throw new UnsupportedOperationException();
        }

        throw new IllegalArgumentException("Unhandled finish " + request.getFinish());
    }
}