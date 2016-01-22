/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.dispatch.OnComplete;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Arrays;
import java.util.List;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
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

    private final ActorContext actorContext;
    private final Future<Object> cohortFuture;
    private final String transactionId;
    private volatile DOMStoreThreePhaseCommitCohort delegateCohort = NoOpDOMStoreThreePhaseCommitCohort.INSTANCE;
    private final OperationCallback.Reference operationCallbackRef;

    SingleCommitCohortProxy(ActorContext actorContext, Future<Object> cohortFuture, String transactionId,
            OperationCallback.Reference operationCallbackRef) {
        this.actorContext = actorContext;
        this.cohortFuture = cohortFuture;
        this.transactionId = transactionId;
        this.operationCallbackRef = operationCallbackRef;
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        LOG.debug("Tx {} canCommit", transactionId);

        final SettableFuture<Boolean> returnFuture = SettableFuture.create();

        cohortFuture.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object cohortResponse) {
                if(failure != null) {
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
        }, actorContext.getClientDispatcher());

        return returnFuture;
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        return delegateCohort.preCommit();
    }

    @Override
    public ListenableFuture<Void> abort() {
        return delegateCohort.abort();
    }

    @Override
    public ListenableFuture<Void> commit() {
        return delegateCohort.commit();
    }

    @Override
    List<Future<Object>> getCohortFutures() {
        return Arrays.asList(cohortFuture);
    }
}
