/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * An AbstractThreePhaseCommitCohort implementation used for debugging. If a failure occurs, the transaction
 * call site is printed.
 *
 * @author Thomas Pantelis
 */
class DebugThreePhaseCommitCohort extends AbstractThreePhaseCommitCohort<Object> {
    private static final Logger LOG = LoggerFactory.getLogger(DebugThreePhaseCommitCohort.class);

    private final AbstractThreePhaseCommitCohort<?> delegate;
    private final Throwable debugContext;
    private final TransactionIdentifier<?> transactionId;
    private Logger log = LOG;

    DebugThreePhaseCommitCohort(TransactionIdentifier<?> transactionId, AbstractThreePhaseCommitCohort<?> delegate,
            Throwable debugContext) {
        this.delegate = Preconditions.checkNotNull(delegate);
        this.debugContext = Preconditions.checkNotNull(debugContext);
        this.transactionId = Preconditions.checkNotNull(transactionId);
    }

    private <V> ListenableFuture<V> addFutureCallback(ListenableFuture<V> future) {
        Futures.addCallback(future, new FutureCallback<V>() {
            @Override
            public void onSuccess(V result) {
                // no-op
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Transaction {} failed with error \"{}\" - was allocated in the following context",
                        transactionId, t, debugContext);
            }
        });

        return future;
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        return addFutureCallback(delegate.canCommit());
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        return addFutureCallback(delegate.preCommit());
    }

    @Override
    public ListenableFuture<Void> commit() {
        return addFutureCallback(delegate.commit());
    }

    @Override
    public ListenableFuture<Void> abort() {
        return delegate.abort();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    List<Future<Object>> getCohortFutures() {
        return ((AbstractThreePhaseCommitCohort)delegate).getCohortFutures();
    }

    @VisibleForTesting
    void setLogger(Logger log) {
        this.log = log;
    }
}
