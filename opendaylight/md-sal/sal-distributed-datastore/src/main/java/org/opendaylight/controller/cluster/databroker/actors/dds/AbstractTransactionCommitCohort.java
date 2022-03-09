/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.common.Empty;

/**
 * Base class for internal {@link DOMStoreThreePhaseCommitCohort} implementation. It contains utility constants for
 * wide reuse.
 *
 * @author Robert Varga
 */
abstract class AbstractTransactionCommitCohort implements DOMStoreThreePhaseCommitCohort {
    static final ListenableFuture<Boolean> TRUE_FUTURE = Futures.immediateFuture(Boolean.TRUE);
    static final ListenableFuture<Empty> EMPTY_FUTURE = Futures.immediateFuture(Empty.value());

    private final AbstractClientHistory parent;
    private final TransactionIdentifier txId;

    AbstractTransactionCommitCohort(final AbstractClientHistory parent, final TransactionIdentifier txId) {
        this.parent = requireNonNull(parent);
        this.txId = requireNonNull(txId);
    }

    final void complete() {
        parent.onTransactionComplete(txId);
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("txId", txId).toString();
    }
}
