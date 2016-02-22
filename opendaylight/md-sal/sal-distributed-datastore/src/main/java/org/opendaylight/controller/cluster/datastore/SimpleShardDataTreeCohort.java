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
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ConflictingModificationAppliedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimpleShardDataTreeCohort extends ShardDataTreeCohort {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleShardDataTreeCohort.class);
    private static final ListenableFuture<Boolean> TRUE_FUTURE = Futures.immediateFuture(Boolean.TRUE);
    private static final ListenableFuture<Void> VOID_FUTURE = Futures.immediateFuture(null);
    private final DataTreeModification transaction;
    private final ShardDataTree dataTree;
    private final String transactionId;
    private DataTreeCandidateTip candidate;

    SimpleShardDataTreeCohort(final ShardDataTree dataTree, final DataTreeModification transaction,
            final String transactionId) {
        this.dataTree = Preconditions.checkNotNull(dataTree);
        this.transaction = Preconditions.checkNotNull(transaction);
        this.transactionId = transactionId;
    }

    @Override
    DataTreeCandidateTip getCandidate() {
        return candidate;
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        DataTreeModification modification = getDataTreeModification();
        try {
            dataTree.getDataTree().validate(modification);
            LOG.trace("Transaction {} validated", transaction);
            return TRUE_FUTURE;
        }
        catch (ConflictingModificationAppliedException e) {
            LOG.warn("Store Tx {}: Conflicting modification for path {}.", transactionId, e.getPath());
            return Futures.immediateFailedFuture(new OptimisticLockFailedException("Optimistic lock failed.", e));
        } catch (DataValidationFailedException e) {
            LOG.warn("Store Tx {}: Data validation failed for path {}.", transactionId, e.getPath(), e);

            // For debugging purposes, allow dumping of the modification. Coupled with the above
            // precondition log, it should allow us to understand what went on.
            LOG.debug("Store Tx {}: modifications: {} tree: {}", transactionId, modification, dataTree.getDataTree());

            return Futures.immediateFailedFuture(new TransactionCommitFailedException("Data did not pass validation.", e));
        } catch (Exception e) {
            LOG.warn("Unexpected failure in validation phase", e);
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        try {
            candidate = dataTree.getDataTree().prepare(getDataTreeModification());
            /*
             * FIXME: this is the place where we should be interacting with persistence, specifically by invoking
             *        persist on the candidate (which gives us a Future).
             */
            LOG.trace("Transaction {} prepared candidate {}", transaction, candidate);
            return VOID_FUTURE;
        } catch (Exception e) {
            if(LOG.isTraceEnabled()) {
                LOG.trace("Transaction {} failed to prepare", transaction, e);
            } else {
                LOG.error("Transaction failed to prepare", e);
            }
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    DataTreeModification getDataTreeModification() {
        DataTreeModification dataTreeModification = transaction;
        if(transaction instanceof PruningDataTreeModification){
            dataTreeModification = ((PruningDataTreeModification) transaction).getResultingModification();
        }
        return dataTreeModification;
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
            if(LOG.isTraceEnabled()) {
                LOG.trace("Transaction {} failed to commit", transaction, e);
            } else {
                LOG.error("Transaction failed to commit", e);
            }
            return Futures.immediateFailedFuture(e);
        }

        LOG.trace("Transaction {} committed, proceeding to notify", transaction);
        dataTree.notifyListeners(candidate);
        return VOID_FUTURE;
    }
}
