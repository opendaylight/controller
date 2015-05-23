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
import org.opendaylight.controller.cluster.datastore.utils.PruningDataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimpleShardDataTreeCohort extends ShardDataTreeCohort {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleShardDataTreeCohort.class);
    private static final ListenableFuture<Boolean> TRUE_FUTURE = Futures.immediateFuture(Boolean.TRUE);
    private static final ListenableFuture<Void> VOID_FUTURE = Futures.immediateFuture(null);
    private final DataTreeModification transaction;
    private final ShardDataTree dataTree;
    private DataTreeCandidateTip candidate;

    SimpleShardDataTreeCohort(final ShardDataTree dataTree, final DataTreeModification transaction) {
        this.dataTree = Preconditions.checkNotNull(dataTree);
        this.transaction = Preconditions.checkNotNull(transaction);
    }

    @Override
    DataTreeCandidateTip getCandidate() {
        return candidate;
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        try {
            dataTree.getDataTree().validate(transaction);
            LOG.debug("Transaction {} validated", transaction);
            return TRUE_FUTURE;
        } catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        try {
            DataTreeModification dataTreeModification = transaction;
            if(transaction instanceof PruningDataTreeModification){
                dataTreeModification = ((PruningDataTreeModification) transaction).getDelegate();
            }

            candidate = dataTree.getDataTree().prepare(dataTreeModification);
            /*
             * FIXME: this is the place where we should be interacting with persistence, specifically by invoking
             *        persist on the candidate (which gives us a Future).
             */
            LOG.debug("Transaction {} prepared candidate {}", transaction, candidate);
            return VOID_FUTURE;
        } catch (Exception e) {
            LOG.debug("Transaction {} failed to prepare", transaction, e);
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public ListenableFuture<Void> abort() {
        // No-op, really
        return VOID_FUTURE;
    }

    @Override
    public ListenableFuture<Void> commit() {
        try {
            dataTree.getDataTree().commit(candidate);
        } catch (Exception e) {
            LOG.error("Transaction {} failed to commit", transaction, e);
            return Futures.immediateFailedFuture(e);
        }

        LOG.debug("Transaction {} committed, proceeding to notify", transaction);
        dataTree.notifyListeners(candidate);
        return VOID_FUTURE;
    }
}
