/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
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
    public ListenableFuture<Void> commit() {
        final ListenableFuture<Void> ret = delegate.commit();

        Futures.addCallback(ret, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                chain.clearTransaction(transaction);
                LOG.debug("Committed transaction {}", transaction);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Transaction {} commit failed, cannot recover", transaction, t);
            }
        });

        return ret;
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        return delegate.canCommit();
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        return delegate.preCommit();
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
}