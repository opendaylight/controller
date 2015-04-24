/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * A local component which is statically bound to a read-only transaction.
 */
final class LocalReadTransactionComponent extends AbstractTransactionComponent {
    private static final Logger LOG = LoggerFactory.getLogger(LocalReadTransactionComponent.class);

    private final DOMStoreReadTransaction delegate;

    LocalReadTransactionComponent(TransactionIdentifier identifier, DOMStoreReadTransaction delegate) {
        super(identifier);
        this.delegate = Preconditions.checkNotNull(delegate);
    }

    @Override
    CheckedFuture<Boolean, ReadFailedException> exists(YangInstanceIdentifier path) {
        LOG.debug("Tx {} exists {}", getIdentifier(), path);
        return delegate.exists(path);
    }

    @Override
    CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(YangInstanceIdentifier path) {
        LOG.debug("Tx {} read {}", getIdentifier(), path);
        return delegate.read(path);
    }

    @Override
    void delete(YangInstanceIdentifier path) {
        throw new UnsupportedOperationException();
    }

    @Override
    void merge(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        throw new UnsupportedOperationException();
    }

    @Override
    void write(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        throw new UnsupportedOperationException();
    }

    @Override
    void close() {
        // Intentional no-op
    }

    @Override
    Future<ActorSelection> coordinatedCommit() {
        throw new UnsupportedOperationException();
    }

    @Override
    AbstractThreePhaseCommitCohort<?> uncoordinatedCommit(final ActorContext actorContext) {
        throw new UnsupportedOperationException();
    }
}
