/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.yang.common.Empty;

/**
 * An {@link AbstractTransactionCommitCohort} for use with empty transactions. This relies on the fact that no backends
 * have been touched, hence all state book-keeping needs to happen only locally and shares fate with the coordinator.
 *
 * <p>Therefore all methods can finish immediately without any effects.
 */
final class EmptyTransactionCommitCohort extends AbstractTransactionCommitCohort {
    EmptyTransactionCommitCohort(final AbstractClientHistory parent, final TransactionIdentifier txId) {
        super(parent, txId);
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        return TRUE_FUTURE;
    }

    @Override
    public ListenableFuture<Empty> preCommit() {
        return Empty.immediateFuture();
    }

    @Override
    public ListenableFuture<Empty> abort() {
        complete();
        return Empty.immediateFuture();
    }

    @Override
    public ListenableFuture<CommitInfo> commit() {
        complete();
        return CommitInfo.emptyFluentFuture();
    }
}
