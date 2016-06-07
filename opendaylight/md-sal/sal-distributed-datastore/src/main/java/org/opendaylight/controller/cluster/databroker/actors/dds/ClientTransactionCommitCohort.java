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
import java.util.Collection;
import java.util.List;
import org.opendaylight.controller.cluster.access.commands.TransactionCanCommitSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ClientTransactionCommitCohort extends AbstractTransactionCommitCohort {
    private static final Logger LOG = LoggerFactory.getLogger(ClientTransactionCommitCohort.class);


    private final List<AbstractProxyTransaction> proxies;
    private List<TransactionCanCommitSuccess> backends;

    /**
     * @param clientTransaction
     */
    ClientTransactionCommitCohort(final Collection<AbstractProxyTransaction> proxies) {
        this.proxies = ImmutableList.copyOf(proxies);
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        /*
         * For empty and single commits there is no action needed.
         *
         * Empty commits will only mark the fact they occurred (for transaction ID tracking inside a local history).
         *
         * Single-entry commits will be committed in preCommit(), with any errors being reported in precommit phase.
         */
        if (proxies.size() <= 1) {
            return TRUE_FUTURE;
        }

        /*
         * Issue the request to commit for all participants. We will track the results and report them.
         */
        final CollectingFuture<Boolean> ret = new CollectingFuture<>(Boolean.TRUE, proxies.size());
        for (AbstractProxyTransaction proxy : proxies) {
            proxy.canCommit(ret);
        }

        return ret;
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        // Just record the transaction id.
        if (proxies.isEmpty()) {
            // FIXME: upcall to ClientLocalHistory to record a skipped transaction.
            return VOID_FUTURE;
        }

        // Single backend, no need to coordinate, perform a direct commit
        if (proxies.size() == 1) {
            return proxies.get(0).directCommit();
        }

        final CollectingFuture<Void> ret = new CollectingFuture<>(null, proxies.size());
        for (AbstractProxyTransaction proxy : proxies) {
            proxy.preCommit(ret);
        }

        return ret;
    }

    @Override
    public ListenableFuture<Void> abort() {



        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<Void> commit() {
        final CollectingFuture<Void> ret = new CollectingFuture<>(null, proxies.size());
        for (AbstractProxyTransaction proxy : proxies) {
            proxy.doCommit(ret);
        }

        return ret;
    }
}