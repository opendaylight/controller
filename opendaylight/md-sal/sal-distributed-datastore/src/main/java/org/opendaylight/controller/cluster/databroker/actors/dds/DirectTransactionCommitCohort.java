/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * An {@link AbstractTransactionCommitCohort} implementation for transactions which contain a single proxy. Since there
 * is only one proxy,
 *
 * @author Robert Varga
 */
final class DirectTransactionCommitCohort extends AbstractTransactionCommitCohort {
    private final AbstractProxyTransaction proxy;

    /**
     * @param clientTransaction
     */
    DirectTransactionCommitCohort(final AbstractProxyTransaction proxy) {
        this.proxy = Preconditions.checkNotNull(proxy);
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        return proxy.directCommit();
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        return VOID_FUTURE;
    }

    @Override
    public ListenableFuture<Void> abort() {
        return VOID_FUTURE;
    }

    @Override
    public ListenableFuture<Void> commit() {
        return VOID_FUTURE;
    }
}
