/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyLocalTransaction;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * Fake {@link DOMStoreThreePhaseCommitCohort} instantiated for local transactions to conform with the DOM
 * transaction APIs. It is only used to hold the data from a local DOM transaction ready operation and to
 * initiate direct or coordinated commits from the front-end by sending the ReadyLocalTransaction message.
 * It is not actually called by the front-end to perform 3PC thus the canCommit/preCommit/commit methods
 * are no-ops.
 */
class LocalThreePhaseCommitCohort implements DOMStoreThreePhaseCommitCohort {
    private static final Logger LOG = LoggerFactory.getLogger(LocalThreePhaseCommitCohort.class);

    private final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction;
    private final DataTreeModification modification;
    private final ActorContext actorContext;
    private final ActorSelection leader;
    private final Exception operationError;

    protected LocalThreePhaseCommitCohort(final ActorContext actorContext, final ActorSelection leader,
            final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction, final DataTreeModification modification) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.leader = Preconditions.checkNotNull(leader);
        this.transaction = Preconditions.checkNotNull(transaction);
        this.modification = Preconditions.checkNotNull(modification);
        this.operationError = null;
    }

    protected LocalThreePhaseCommitCohort(final ActorContext actorContext, final ActorSelection leader,
            final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction, final Exception operationError) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.leader = Preconditions.checkNotNull(leader);
        this.transaction = Preconditions.checkNotNull(transaction);
        this.operationError = Preconditions.checkNotNull(operationError);
        this.modification = null;
    }

    private Future<Object> initiateCommit(final boolean immediate) {
        if(operationError != null) {
            return Futures.failed(operationError);
        }

        final ReadyLocalTransaction message = new ReadyLocalTransaction(transaction.getIdentifier().toString(),
                modification, immediate);
        return actorContext.executeOperationAsync(leader, message, actorContext.getTransactionCommitOperationTimeout());
    }

    Future<ActorSelection> initiateCoordinatedCommit() {
        final Future<Object> messageFuture = initiateCommit(false);
        final Future<ActorSelection> ret = TransactionReadyReplyMapper.transform(messageFuture, actorContext,
                transaction.getIdentifier());
        ret.onComplete(new OnComplete<ActorSelection>() {
            @Override
            public void onComplete(final Throwable failure, final ActorSelection success) throws Throwable {
                if (failure != null) {
                    LOG.info("Failed to prepare transaction {} on backend", transaction.getIdentifier(), failure);
                    transactionAborted(transaction);
                    return;
                }

                LOG.debug("Transaction {} resolved to actor {}", transaction.getIdentifier(), success);
            }
        }, actorContext.getClientDispatcher());

        return ret;
    }

    Future<Object> initiateDirectCommit() {
        final Future<Object> messageFuture = initiateCommit(true);
        messageFuture.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable failure, final Object message) throws Throwable {
                if (failure != null) {
                    LOG.error("Failed to prepare transaction {} on backend", transaction.getIdentifier(), failure);
                    transactionAborted(transaction);
                } else if (CommitTransactionReply.isSerializedType(message)) {
                    LOG.debug("Transaction {} committed successfully", transaction.getIdentifier());
                    transactionCommitted(transaction);
                } else {
                    LOG.error("Transaction {} resulted in unhandled message type {}, aborting", message.getClass());
                    transactionAborted(transaction);
                }
            }
        }, actorContext.getClientDispatcher());

        return messageFuture;
    }

    @Override
    public final ListenableFuture<Boolean> canCommit() {
        // Intended no-op
        throw new UnsupportedOperationException();
    }

    @Override
    public final ListenableFuture<Void> preCommit() {
        // Intended no-op
        throw new UnsupportedOperationException();
    }

    @Override
    public final ListenableFuture<Void> abort() {
        // Intended no-op
        throw new UnsupportedOperationException();
    }

    @Override
    public final ListenableFuture<Void> commit() {
        // Intended no-op
        throw new UnsupportedOperationException();
    }

    protected void transactionAborted(SnapshotBackedWriteTransaction<TransactionIdentifier> transaction) {
    }

    protected void transactionCommitted(SnapshotBackedWriteTransaction<TransactionIdentifier> transaction) {
    }
}
