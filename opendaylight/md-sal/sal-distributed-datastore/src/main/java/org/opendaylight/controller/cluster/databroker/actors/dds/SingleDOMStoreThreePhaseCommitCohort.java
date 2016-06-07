/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;

/**
 * Frontend implementation of a commit cohort for use with a single backend. This implementation is not completely
 * corrent, but relies on the that ConcurrentDOMDataBroker does not have any further coordinators. This allows this
 * implementation to fire a single message at the backend and complete it. This happens at the 'preCommit()' step.
 *
 * @author Robert Varga
 */
@NotThreadSafe
final class SingleDOMStoreThreePhaseCommitCohort implements DOMStoreThreePhaseCommitCohort {
    private final AbstractProxyTransaction tx;
    private ListenableFuture<Void> precommitFuture;

    SingleDOMStoreThreePhaseCommitCohort(final AbstractProxyTransaction tx) {
        this.tx = Preconditions.checkNotNull(tx);
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        Preconditions.checkState(precommitFuture == null, "Transaction has already been precommitted");

        // We pretend this succeeds
        return NoOpDOMStoreThreePhaseCommitCohort.IMMEDIATE_BOOLEAN_SUCCESS;
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        Preconditions.checkState(precommitFuture == null, "Transaction has already been precommitted");
        precommitFuture = Futures.transform(tx.commit(false), (Function<TransactionSuccess<?>, Void>)t -> null);
        return precommitFuture;
    }

    @Override
    public ListenableFuture<Void> abort() {
        if (precommitFuture != null) {
            throw new UnsupportedOperationException("Single transactions cannot be aborted after initition");
        }

        tx.abort();
        return NoOpDOMStoreThreePhaseCommitCohort.IMMEDIATE_VOID_SUCCESS;
    }

    @Override
    public ListenableFuture<Void> commit() {
        Preconditions.checkState(precommitFuture != null, "Transaction has not been precommitted yet");
        return precommitFuture;
    }
}
