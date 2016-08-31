/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

final class ClientTransactionCommitCohort extends AbstractTransactionCommitCohort {
    private final Collection<AbstractProxyTransaction> proxies;

    /**
     * @param clientTransaction
     */
    ClientTransactionCommitCohort(final AbstractClientHistory parent, final TransactionIdentifier txId,
        final Collection<AbstractProxyTransaction> proxies) {
        super(parent, txId);
        this.proxies = ImmutableList.copyOf(proxies);
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        /*
         * Issue the request to commit for all participants. We will track the results and report them.
         */
        final VotingFuture<Boolean> ret = new VotingFuture<>(Boolean.TRUE, proxies.size());
        for (AbstractProxyTransaction proxy : proxies) {
            proxy.canCommit(ret);
        }

        return ret;
    }

    private ListenableFuture<Void> addComplete(final ListenableFuture<Void> future) {
        future.addListener(this::complete, MoreExecutors.directExecutor());
        return future;
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        final VotingFuture<Void> ret = new VotingFuture<>(null, proxies.size());
        for (AbstractProxyTransaction proxy : proxies) {
            proxy.preCommit(ret);
        }

        return ret;
    }

    @Override
    public ListenableFuture<Void> commit() {
        final VotingFuture<Void> ret = new VotingFuture<>(null, proxies.size());
        for (AbstractProxyTransaction proxy : proxies) {
            proxy.doCommit(ret);
        }

        return addComplete(ret);
    }

    @Override
    public ListenableFuture<Void> abort() {
        final VotingFuture<Void> ret = new VotingFuture<>(null, proxies.size());
        for (AbstractProxyTransaction proxy : proxies) {
            proxy.abort(ret);
        }

        return addComplete(ret);
    }
}