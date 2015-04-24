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
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.ReadyLocalTransaction;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * Fake {@link DOMStoreThreePhaseCommitCohort} instantiated for local transactions.
 * Its only function is to leak the component data to {@link LocalWritableTransactionComponent},
 * which picks it up and uses it to communicate with the shard leader.
 */
abstract class LocalThreePhaseCommitCohort extends AbstractThreePhaseCommitCohort<ActorSelection> {
    private static final Logger LOG = LoggerFactory.getLogger(LocalThreePhaseCommitCohort.class);
    private final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction;
    private final DataTreeModification modification;
    private final ActorContext actorContext;
    private final ActorSelection leader;
    private List<Future<ActorSelection>> cohortFutures;
    private DOMStoreThreePhaseCommitCohort delegate;

    protected LocalThreePhaseCommitCohort(final ActorContext actorContext, final ActorSelection leader,
            final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction, final DataTreeModification modification) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.leader = Preconditions.checkNotNull(leader);
        this.transaction = Preconditions.checkNotNull(transaction);
        this.modification = Preconditions.checkNotNull(modification);
    }

    Future<ActorSelection> initiateCommit(final boolean immediate) {
        Preconditions.checkState(cohortFutures == null, "Cohort for transaction {} has already been initiated", transaction);

        final ReadyLocalTransaction message = new ReadyLocalTransaction(transaction.getIdentifier().toString(), modification, immediate);
        final Future<Object> messageFuture = actorContext.executeOperationAsync(leader, message);

        final Future<ActorSelection> ret = TransactionReadyReplyMapper.transform(messageFuture, actorContext, transaction.getIdentifier());
        ret.onComplete(new OnComplete<ActorSelection>() {
            @Override
            public void onComplete(final Throwable failure, final ActorSelection success) throws Throwable {
                if (failure != null) {
                    LOG.info("Failed to prepare transaction {} on backend", transaction.getIdentifier(), failure);
                    transactionAborted(transaction);
                    return;
                }

                LOG.debug("Transaction {} resolved to actor {}", transaction.getIdentifier(), success);
                if (immediate) {
                    transactionCommitted(transaction);
                }
            }
        }, actorContext.getClientDispatcher());

        cohortFutures = Collections.singletonList(ret);

        if (immediate) {
            delegate = DirectCommitCohort.create(actorContext, ret);
        } else {
            delegate = NoOpDOMStoreThreePhaseCommitCohort.INSTANCE;
        }
        return ret;
    }

    private void checkInitiated() {
        Preconditions.checkState(cohortFutures != null, "Commit process for transaction %s has not been initiated", transaction);
    }

    @Override
    public final ListenableFuture<Boolean> canCommit() {
        checkInitiated();
        return delegate.canCommit();
    }

    @Override
    public final ListenableFuture<Void> preCommit() {
        checkInitiated();
        return delegate.preCommit();
    }

    @Override
    public final ListenableFuture<Void> abort() {
        checkInitiated();
        transactionAborted(transaction);
        return delegate.abort();
    }

    @Override
    public final ListenableFuture<Void> commit() {
        checkInitiated();
        transactionCommitted(transaction);
        return delegate.commit();
    }

    @Override
    final List<Future<ActorSelection>> getCohortFutures() {
        checkInitiated();
        return cohortFutures;
    }

    protected abstract void transactionAborted(SnapshotBackedWriteTransaction<TransactionIdentifier> transaction);
    protected abstract void transactionCommitted(SnapshotBackedWriteTransaction<TransactionIdentifier> transaction);
}
