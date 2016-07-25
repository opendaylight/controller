/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ChainedCommitCohort extends ShardDataTreeCohort {
    private static final Logger LOG = LoggerFactory.getLogger(ChainedCommitCohort.class);
    private final ReadWriteShardDataTreeTransaction transaction;
    private final ShardDataTreeTransactionChain chain;
    private final ShardDataTreeCohort delegate;

    ChainedCommitCohort(final ShardDataTreeTransactionChain chain, final ReadWriteShardDataTreeTransaction transaction, final ShardDataTreeCohort delegate) {
        this.transaction = Preconditions.checkNotNull(transaction);
        this.delegate = Preconditions.checkNotNull(delegate);
        this.chain = Preconditions.checkNotNull(chain);
    }

    @Override
    public void commit(final FutureCallback<UnsignedLong> callback) {
        delegate.commit(new FutureCallback<UnsignedLong>() {
            @Override
            public void onSuccess(final UnsignedLong result) {
                chain.clearTransaction(transaction);
                LOG.debug("Committed transaction {}", transaction);
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Transaction {} commit failed, cannot recover", transaction, t);
                callback.onFailure(t);
            }
        });
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public void canCommit(final FutureCallback<Void> callback) {
        delegate.canCommit(callback);
    }

    @Override
    public void preCommit(final FutureCallback<DataTreeCandidate> callback) {
        delegate.preCommit(callback);
    }

    @Override
    public ListenableFuture<Void> abort() {
        return delegate.abort();
    }

    @Override
    DataTreeCandidateTip getCandidate() {
        return delegate.getCandidate();
    }

    @Override
    DataTreeModification getDataTreeModification() {
        return delegate.getDataTreeModification();
    }

    @Override
    public boolean isFailed() {
        return delegate.isFailed();
    }

    @Override
    public State getState() {
        return delegate.getState();
    }
}