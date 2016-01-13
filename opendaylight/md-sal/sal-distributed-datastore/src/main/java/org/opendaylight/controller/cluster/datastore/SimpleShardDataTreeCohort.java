/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opendaylight.controller.cluster.datastore.utils.PruningDataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.mdsal.common.api.PostCanCommitStep;
import org.opendaylight.mdsal.common.api.PostPreCommitStep;
import org.opendaylight.mdsal.common.api.ThreePhaseCommitStep;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ConflictingModificationAppliedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
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
    private SchemaContext schema;
    private Collection<? extends ThreePhaseCommitStep> activeCohorts;
    private ResultCollection<PostPreCommitStep> userPreCommitFinished;

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
        DataTreeModification modification = dataTreeModification();
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
            // FIXME: Should candidate creation be moved to canCommit?
            candidate = dataTree.getDataTree().prepare(dataTreeModification());
            ResultCollection<PostCanCommitStep> postCustomCanCommit = customCohortsCanCommit().get();
            activeCohorts = postCustomCanCommit.getValues();
            if (!postCustomCanCommit.getThrowables().isEmpty()) {
                return Futures.immediateFailedFuture(cohortsCanCommitFailure(postCustomCanCommit.getThrowables()));
            }
            userPreCommitFinished = customCohortsPreCommit(postCustomCanCommit.getValues()).get();
            activeCohorts = userPreCommitFinished.getValues();

            if (!userPreCommitFinished.getThrowables().isEmpty()) {
                return Futures.immediateFailedFuture(cohortsPreCommitFailure(userPreCommitFinished.getThrowables()));
            }
            /*
             * FIXME: this is the place where we should be interacting with persistence, specifically by invoking
             *        persist on the candidate (which gives us a Future).
             */
            LOG.trace("Transaction {} prepared candidate {}", transaction, candidate);
            return VOID_FUTURE;
        } catch (Exception e) {
            LOG.debug("Transaction {} failed to prepare", transaction, e);
            return Futures.immediateFailedFuture(e);
        }
    }

    private ListenableFuture<ResultCollection<PostCanCommitStep>> customCohortsCanCommit() {
        return dataTree.userCohorsCanCommit(transactionId, candidate, schema);
    }

    private ListenableFuture<ResultCollection<PostPreCommitStep>> customCohortsPreCommit(
            Collection<PostCanCommitStep> steps) {
        Collection<ListenableFuture<? extends PostPreCommitStep>> preCommitFutures = new ArrayList<>(steps.size());
        for (PostCanCommitStep commitStep : steps) {
            preCommitFutures.add(commitStep.preCommit());
        }
        return ResultCollection.fromFutures(preCommitFutures);
    }

    private ListenableFuture<ResultCollection<Object>> cohortsCommit() {
        Collection<PostPreCommitStep> postPreCommits = userPreCommitFinished.getValues();
        List<ListenableFuture<?>> commitFutures = new ArrayList<>(postPreCommits.size());
        for (PostPreCommitStep postPreCommit : postPreCommits) {
            commitFutures.add(postPreCommit.commit());
        }
        return ResultCollection.fromFutures(commitFutures);
    }

    private Throwable cohortsCanCommitFailure(Collection<Throwable> throwables) {
        TransactionCommitFailedException frontend =
                new TransactionCommitFailedException("Custom cohorts canCommit failed.");
        for (Throwable backend : throwables) {
            frontend.addSuppressed(backend);
        }
        return frontend;
    }

    private Throwable cohortsPreCommitFailure(Collection<Throwable> throwables) {
        TransactionCommitFailedException frontend =
                new TransactionCommitFailedException("Custom cohorts preCommit failed.");
        for (Throwable backend : throwables) {
            frontend.addSuppressed(backend);
        }
        return frontend;
    }

    private DataTreeModification dataTreeModification() {
        DataTreeModification dataTreeModification = transaction;
        if(transaction instanceof PruningDataTreeModification){
            dataTreeModification = ((PruningDataTreeModification) transaction).getDelegate();
        }
        return dataTreeModification;
    }

    @Override
    public ListenableFuture<Void> abort() {
        if (activeCohorts == null) {
            return VOID_FUTURE;
        }
        Collection<ListenableFuture<?>> userAbortFutures = new ArrayList<>(activeCohorts.size());
        for (ThreePhaseCommitStep value : activeCohorts) {
            userAbortFutures.add(value.abort());
        }
        return Futures.transform(ResultCollection.fromFutures(userAbortFutures),
                new Function<ResultCollection<?>, Void>() {

                    @Override
                    public Void apply(ResultCollection<?> input) {
                        return null;
                    }
                });
    }

    @Override
    public ListenableFuture<Void> commit() {
        try {
            dataTree.getDataTree().commit(candidate);
        } catch (Exception e) {
            LOG.error("Transaction {} failed to commit", transaction, e);
            return Futures.immediateFailedFuture(e);
        }


        cohortsCommit();
        LOG.trace("Transaction {} committed, proceeding to notify", transaction);
        dataTree.notifyListeners(candidate);
        return VOID_FUTURE;
    }

}
