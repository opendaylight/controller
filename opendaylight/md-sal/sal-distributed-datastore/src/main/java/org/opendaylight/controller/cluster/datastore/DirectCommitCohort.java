/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.dispatch.OnComplete;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import scala.concurrent.Future;

/**
 * A {@link DOMStoreThreePhaseCommitCohort} which reports results as obtained by performing
 * a direct commit on the backend. If resolution of actor succeeds, the transaction is assumed
 * as having been committed.
 */
final class DirectCommitCohort implements DOMStoreThreePhaseCommitCohort {
    private final SettableFuture<Boolean> canCommit;
    private final SettableFuture<Void> others;

    private DirectCommitCohort(final SettableFuture<Boolean> canCommit, final SettableFuture<Void> others) {
        this.canCommit = Preconditions.checkNotNull(canCommit);
        this.others = Preconditions.checkNotNull(others);
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        return canCommit;
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        return others;
    }

    @Override
    public ListenableFuture<Void> abort() {
        return others;
    }

    @Override
    public ListenableFuture<Void> commit() {
        return others;
    }

    static DOMStoreThreePhaseCommitCohort create(final ActorContext actorContext, final Future<Object> ret) {
        final SettableFuture<Boolean> canCommit = SettableFuture.create();
        final SettableFuture<Void> others = SettableFuture.create();

        ret.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable failure, final Object unused) {
                if (failure != null) {
                    others.setException(failure);
                    canCommit.set(Boolean.FALSE);
                } else {
                    others.set(null);
                    canCommit.set(Boolean.TRUE);
                }
            }
        }, actorContext.getClientDispatcher());

        return new DirectCommitCohort(canCommit, others);
    }
}
