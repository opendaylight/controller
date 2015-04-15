/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import com.google.common.util.concurrent.FutureCallback;
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
 * to the shard, this implementation elides the 3-phase commit. Otherwise the 3-phase commit is performed.
 *
 * @author Thomas Pantelis
 */
class SingleCommitCohortProxy extends AbstractThreePhaseCommitCohort<Object> {
    private static final Logger LOG = LoggerFactory.getLogger(SingleCommitCohortProxy.class);

    private final ActorContext actorContext;
    private final Future<Object> cohortFuture;
    private final String transactionId;
    private volatile DOMStoreThreePhaseCommitCohort delegateCohort = NoOpDOMStoreThreePhaseCommitCohort.INSTANCE;

    SingleCommitCohortProxy(ActorContext actorContext, Future<Object> cohortFuture, String transactionId) {
        this.actorContext = actorContext;
        this.cohortFuture = cohortFuture;
        this.transactionId = transactionId;
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        LOG.debug("Tx {} canCommit", transactionId);

        final SettableFuture<Boolean> returnFuture = SettableFuture.create();

        cohortFuture.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object cohortResponse) {
                if(failure != null) {
                    returnFuture.setException(failure);
                    return;
                }

                if(cohortResponse instanceof ActorSelection) {
                    handlePreLithiumCohort((ActorSelection)cohortResponse, returnFuture);
                    return;
                }

                LOG.debug("Tx {} successfully completed direct commit", transactionId);

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

    private void handlePreLithiumCohort(ActorSelection actorSelection, final SettableFuture<Boolean> returnFuture) {
        // Handle backwards compatibility. An ActorSelection response would be returned from a
        // pre-Lithium controller. In this case delegate to a ThreePhaseCommitCohortProxy.
        delegateCohort = new ThreePhaseCommitCohortProxy(actorContext,
                Arrays.asList(Futures.successful(actorSelection)), transactionId);
        com.google.common.util.concurrent.Futures.addCallback(delegateCohort.canCommit(), new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean canCommit) {
                returnFuture.set(canCommit);
            }

            @Override
            public void onFailure(Throwable t) {
                returnFuture.setException(t);
            }
        });
    }
}
