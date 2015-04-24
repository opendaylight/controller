/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

/**
 * Fake {@link DOMStoreThreePhaseCommitCohort} instantiated for local transactions.
 * Its only function is to leak the component data to {@link LocalWritableTransactionComponent},
 * which picks it up and uses it to communicate with the shard leader.
 */
final class LocalThreePhaseCommitCohort implements DOMStoreThreePhaseCommitCohort {
    private final DataTreeModification modification;
    private final TransactionIdentifier identifier;
    private final ActorContext actorContext;

    public LocalThreePhaseCommitCohort(ActorContext actorContext, TransactionIdentifier identifier,
            DataTreeModification modification) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.identifier = Preconditions.checkNotNull(identifier);
        this.modification = Preconditions.checkNotNull(modification);
    }

    TransactionIdentifier getIdentifier() {
        return identifier;
    }

    DataTreeModification getModification() {
        return modification;
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        throw new UnsupportedOperationException("Should never be called");
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        throw new UnsupportedOperationException("Should never be called");
    }

    @Override
    public ListenableFuture<Void> abort() {
        throw new UnsupportedOperationException("Should never be called");
    }

    @Override
    public ListenableFuture<Void> commit() {
        throw new UnsupportedOperationException("Should never be called");
    }
}
