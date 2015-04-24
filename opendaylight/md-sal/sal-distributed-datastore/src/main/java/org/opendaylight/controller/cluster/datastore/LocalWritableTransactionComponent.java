/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * Local transaction component which is writable and therefore needs to be committed
 * on the remote shard.
 *
 * @param <T> transaction type
 */
abstract class LocalWritableTransactionComponent<T extends DOMStoreWriteTransaction> extends AbstractTransactionComponent {
    private static final Logger LOG = LoggerFactory.getLogger(LocalWritableTransactionComponent.class);

    private final T delegate;

    protected LocalWritableTransactionComponent(TransactionIdentifier identifier, final T delegate) {
        super(identifier);
        this.delegate = Preconditions.checkNotNull(delegate);
    }

    protected final T delegate() {
        return delegate;
    }

    @Override
    final void close() {
        delegate.close();
    }

    private LocalThreePhaseCommitCohort ready() {
        return (LocalThreePhaseCommitCohort) delegate.ready();
    }

    @Override
    final Future<ActorSelection> coordinatedCommit() {
        LOG.debug("Tx {} coordinatedCommit", getIdentifier());
        final LocalThreePhaseCommitCohort cohort = ready();
        return cohort.initiateCoordinatedCommit();
    }

    @Override
    final AbstractThreePhaseCommitCohort<ActorSelection> uncoordinatedCommit(final ActorContext actorContext) {
        LOG.debug("Tx {} uncoordinatedCommit", getIdentifier());
        final LocalThreePhaseCommitCohort ret = ready();
        ret.initiateDirectCommit();
        return ret;
    }
}
