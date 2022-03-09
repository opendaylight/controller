/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.yang.common.Empty;

/**
 * An {@link AbstractTransactionCommitCohort} implementation for transactions which contain a single proxy. Since there
 * is only one proxy,
 *
 * @author Robert Varga
 */
final class DirectTransactionCommitCohort extends AbstractTransactionCommitCohort {
    private final AbstractProxyTransaction proxy;

    DirectTransactionCommitCohort(final AbstractClientHistory parent, final TransactionIdentifier txId,
        final AbstractProxyTransaction proxy) {
        super(parent, txId);
        this.proxy = requireNonNull(proxy);
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        return proxy.directCommit();
    }

    @Override
    public ListenableFuture<Empty> preCommit() {
        return EMPTY_FUTURE;
    }

    @Override
    public ListenableFuture<Empty> abort() {
        complete();
        return EMPTY_FUTURE;
    }

    @Override
    public ListenableFuture<CommitInfo> commit() {
        complete();
        return CommitInfo.emptyFluentFuture();
    }
}
