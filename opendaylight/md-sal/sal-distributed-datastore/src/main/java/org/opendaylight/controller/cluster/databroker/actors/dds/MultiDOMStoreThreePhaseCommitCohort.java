/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.commands.TransactionCanCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend implementation of a commit cohort for use with multiple backends.
 *
 * This implementation fires off a commit request at instantiation time, waiting for a transaction handle, which is
 * the result of a 'canCommit' step at the backend.
 *
 * @author Robert Varga
 */
@NotThreadSafe
final class MultiDOMStoreThreePhaseCommitCohort implements DOMStoreThreePhaseCommitCohort {
    private static final Logger LOG = LoggerFactory.getLogger(MultiDOMStoreThreePhaseCommitCohort.class);

    private final SettableFuture<Boolean> cancommitFuture = SettableFuture.create();
    private volatile List<TransactionCanCommitSuccess> resolvedHandles;

    MultiDOMStoreThreePhaseCommitCohort(final Collection<AbstractProxyTransaction> txns) {
        final List<ListenableFuture<TransactionSuccess<?>>> futures = new ArrayList<>(txns.size());

        for (AbstractProxyTransaction t : txns) {
            futures.add(t.commit(true));
        }

        /**
         * Gather the futures for processing. Once all of them completed, we will evaluate if all of them
         * succeeded. If any have failed we will transmit abort on the successful ones.
         */
        final ListenableFuture<List<TransactionSuccess<?>>> futureHandles = Futures.successfulAsList(futures);
        Futures.addCallback(futureHandles, new FutureCallback<List<TransactionSuccess<?>>>() {
            @Override
            public void onSuccess(final List<TransactionSuccess<?>> result) {
                final List<TransactionCanCommitSuccess> tmp = new ArrayList<>(result.size());
                boolean approved = true;

                for (TransactionSuccess<?> success : result) {
                    if (success instanceof TransactionCanCommitSuccess) {
                        tmp.add((TransactionCanCommitSuccess) success);
                        continue;
                    }

                    if (success != null) {
                        LOG.warn("Unexpected success message {}, failing commit", success);
                    }
                    approved = false;
                }

                resolvedHandles = ImmutableList.copyOf(tmp);
                cancommitFuture.set(approved);
            }

            @Override
            public void onFailure(final Throwable t) {
                cancommitFuture.setException(t);
            }
        });
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        return cancommitFuture;
    }

    private List<TransactionCanCommitSuccess> getHandles() {
        Preconditions.checkState(resolvedHandles != null, "Handles have not been gathered yet");
        return resolvedHandles;
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        final List<TransactionCanCommitSuccess> handles = getHandles();

        // FIXME: finish this

        return null;
    }

    @Override
    public ListenableFuture<Void> abort() {
        final List<TransactionCanCommitSuccess> handles = getHandles();

        // FIXME: finish this

        return null;
    }

    @Override
    public ListenableFuture<Void> commit() {
        final List<TransactionCanCommitSuccess> handles = getHandles();

        // FIXME: finish this

        return null;
    }
}
