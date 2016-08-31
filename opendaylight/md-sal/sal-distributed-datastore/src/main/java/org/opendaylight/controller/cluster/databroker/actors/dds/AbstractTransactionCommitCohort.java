/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;

/**
 * Base class for internal {@link DOMStoreThreePhaseCommitCohort} implementation. It contains utility constants for
 * wide reuse.
 *
 * @author Robert Varga
 */
abstract class AbstractTransactionCommitCohort implements DOMStoreThreePhaseCommitCohort {
    static final ListenableFuture<Boolean> TRUE_FUTURE = Futures.immediateFuture(Boolean.TRUE);
    static final ListenableFuture<Void> VOID_FUTURE = Futures.immediateFuture(null);

    private final AbstractClientHistory parent;
    private final TransactionIdentifier txId;

    AbstractTransactionCommitCohort(final AbstractClientHistory parent, final TransactionIdentifier txId) {
        this.parent = Preconditions.checkNotNull(parent);
        this.txId = Preconditions.checkNotNull(txId);
    }

    final void complete() {
        parent.onTransactionComplete(txId);
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("txId", txId).toString();
    }
}
