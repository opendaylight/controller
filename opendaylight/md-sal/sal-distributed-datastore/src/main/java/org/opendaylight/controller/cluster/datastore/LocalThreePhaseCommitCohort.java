/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

/**
 * Fake {@link DOMStoreThreePhaseCommitCohort} instantiated for local transactions.
 * Its only function is to leak the component data to {@link LocalWritableTransactionComponent},
 * which picks it up and uses it to communicate with the shard leader.
 */
abstract class LocalThreePhaseCommitCohort implements DOMStoreThreePhaseCommitCohort {
    private static final ListenableFuture<Boolean> TRUE_FUTURE = Futures.immediateFuture(Boolean.TRUE);
    private static final ListenableFuture<Void> NULL_FUTURE = Futures.immediateFuture(null);
    private final DataTreeModification modification;
    private final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction;
    private final ActorContext actorContext;

    protected LocalThreePhaseCommitCohort(ActorContext actorContext, SnapshotBackedWriteTransaction<TransactionIdentifier> transaction,
            DataTreeModification modification) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.transaction = Preconditions.checkNotNull(transaction);
        this.modification = Preconditions.checkNotNull(modification);
    }

    final TransactionIdentifier getIdentifier() {
        return transaction.getIdentifier();
    }

    final DataTreeModification getModification() {
        return modification;
    }

    final ActorContext getActorContext() {
        return actorContext;
    }

    @Override
    public final ListenableFuture<Boolean> canCommit() {
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
