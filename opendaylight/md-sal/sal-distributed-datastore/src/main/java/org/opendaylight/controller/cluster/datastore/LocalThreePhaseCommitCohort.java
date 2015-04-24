/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.OnComplete;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.ReadyLocalTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * Fake {@link DOMStoreThreePhaseCommitCohort} instantiated for local transactions.
 * Its only function is to leak the component data to {@link LocalWritableTransactionComponent},
 * which picks it up and uses it to communicate with the shard leader.
 */
abstract class LocalThreePhaseCommitCohort extends AbstractThreePhaseCommitCohort<ActorSelection> {
    private static final Logger LOG = LoggerFactory.getLogger(LocalThreePhaseCommitCohort.class);
    private static final ListenableFuture<Boolean> TRUE_FUTURE = Futures.immediateFuture(Boolean.TRUE);
    private static final ListenableFuture<Void> NULL_FUTURE = Futures.immediateFuture(null);
    private final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction;
    private final DataTreeModification modification;
    private final ActorContext actorContext;
    private final ActorSelection leader;

    protected LocalThreePhaseCommitCohort(final ActorContext actorContext, final ActorSelection leader,
            final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction, final DataTreeModification modification) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.leader = Preconditions.checkNotNull(leader);
        this.transaction = Preconditions.checkNotNull(transaction);
        this.modification = Preconditions.checkNotNull(modification);
    }

    /**
     * Turn this cohort into an immediately-committed one. This will modify the message we
     * send to the shard leader and how we react to the response we get back.
     */
    final void makeImmediate() {
    }

    Future<ActorSelection> initiateCommit(final boolean immediate) {
        final ReadyLocalTransaction message = new ReadyLocalTransaction(transaction.getIdentifier().toString(), modification, immediate);
        final Future<Object> messageFuture = actorContext.executeOperationAsync(leader, message);

        return TransactionReadyReplyMapper.transform(messageFuture, actorContext, transaction.getIdentifier());
    }

    @Override
    public final ListenableFuture<Boolean> canCommit() {

        final SettableFuture<Boolean> ret = SettableFuture.create();
        messageFuture.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable failure, final Object success) {
                if (failure != null) {
                    LOG.info("Failed to prepare transaction {} on backend", transaction.getIdentifier(), failure);
                    ret.set(Boolean.FALSE);
                    transactionAborted(transaction);
                    return;
                }

                // success should be ReadyTransactionReply. When we receive it, it means
                // the transaction can go through.
                final ReadyTransactionReply reply = (ReadyTransactionReply) success;
                ret.set(Boolean.TRUE);

                // FIXME: next up


                // FIXME: we need to do something about the response


                // If this is not an immediate commit, we will need the actor reference
                // to the actor which handles the next stage of the commit process.
                if (!immediate) {
                    transactionCommitted(transaction);
                }
            }
        }, actorContext.getClientDispatcher());


        return ret;


        final Promise<ActorSelection> promise = akka.dispatch.Futures.promise();
        txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(final TransactionContext transactionContext) {
                promise.completeWith(transactionContext.readyTransaction());
            }
        });

        return promise.future();

        return TRUE_FUTURE;
    }

    @Override
    public final ListenableFuture<Void> preCommit() {
        return NULL_FUTURE;
    }

    @Override
    public final ListenableFuture<Void> abort() {
        transactionAborted(transaction);
        return NULL_FUTURE;
    }

    @Override
    public final ListenableFuture<Void> commit() {
        transactionCommitted(transaction);
        return NULL_FUTURE;
    }

    protected abstract void transactionAborted(SnapshotBackedWriteTransaction<TransactionIdentifier> transaction);
    protected abstract void transactionCommitted(SnapshotBackedWriteTransaction<TransactionIdentifier> transaction);
}
