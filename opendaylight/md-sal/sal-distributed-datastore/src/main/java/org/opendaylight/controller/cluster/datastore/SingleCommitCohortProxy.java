/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.dispatch.OnComplete;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.List;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.common.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * A cohort proxy implementation for a single-shard transaction commit. If the transaction was a direct commit
 * to the shard, this implementation elides the CanCommitTransaction and CommitTransaction messages to the
 * shard as an optimization.
 *
 * @author Thomas Pantelis
 */
class SingleCommitCohortProxy extends AbstractThreePhaseCommitCohort<Object> {
    private static final Logger LOG = LoggerFactory.getLogger(SingleCommitCohortProxy.class);

    private final ActorUtils actorUtils;
    private final Future<Object> cohortFuture;
    private final TransactionIdentifier transactionId;
    private volatile DOMStoreThreePhaseCommitCohort delegateCohort = NoOpDOMStoreThreePhaseCommitCohort.INSTANCE;
    private final OperationCallback.Reference operationCallbackRef;

    SingleCommitCohortProxy(final ActorUtils actorUtils, final Future<Object> cohortFuture,
            final TransactionIdentifier transactionId, final OperationCallback.Reference operationCallbackRef) {
        this.actorUtils = actorUtils;
        this.cohortFuture = cohortFuture;
        this.transactionId = requireNonNull(transactionId);
        this.operationCallbackRef = operationCallbackRef;
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        LOG.debug("Tx {} canCommit", transactionId);

        final SettableFuture<Boolean> returnFuture = SettableFuture.create();

        cohortFuture.onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object cohortResponse) {
                if (failure != null) {
                    operationCallbackRef.get().failure();
                    returnFuture.setException(failure);
                    return;
                }

                operationCallbackRef.get().success();

                LOG.debug("Tx {} successfully completed direct commit", transactionId);

                // The Future was the result of a direct commit to the shard, essentially eliding the
                // front-end 3PC coordination. We don't really care about the specific Future
                // response object, only that it completed successfully. At this point the Tx is complete
                // so return true. The subsequent preCommit and commit phases will be no-ops, ie return
                // immediate success, to complete the 3PC for the front-end.
                returnFuture.set(Boolean.TRUE);
            }
        }, actorUtils.getClientDispatcher());

        return returnFuture;
    }

    @Override
    public ListenableFuture<Empty> preCommit() {
        return delegateCohort.preCommit();
    }

    @Override
    public ListenableFuture<Empty> abort() {
        return delegateCohort.abort();
    }

    @Override
    public ListenableFuture<? extends CommitInfo> commit() {
        return delegateCohort.commit();
    }

    @Override
    List<Future<Object>> getCohortFutures() {
        return List.of(cohortFuture);
    }
}
