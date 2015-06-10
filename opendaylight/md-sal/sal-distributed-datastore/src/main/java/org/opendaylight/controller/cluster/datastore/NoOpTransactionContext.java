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
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.DataStoreUnavailableException;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

final class NoOpTransactionContext extends AbstractTransactionContext {
    private static final Logger LOG = LoggerFactory.getLogger(NoOpTransactionContext.class);

    private final Throwable failure;

    public NoOpTransactionContext(Throwable failure, TransactionIdentifier identifier) {
        super(identifier);
        this.failure = failure;
    }

    @Override
    public void closeTransaction() {
        LOG.debug("NoOpTransactionContext {} closeTransaction called", getIdentifier());
    }

    @Override
    public boolean supportsDirectCommit() {
        return true;
    }

    @Override
    public Future<Object> directCommit() {
        LOG.debug("Tx {} directCommit called, failure: {}", getIdentifier(), failure);
        return akka.dispatch.Futures.failed(failure);
    }

    @Override
    public Future<ActorSelection> readyTransaction() {
        LOG.debug("Tx {} readyTransaction called, failure: {}", getIdentifier(), failure);
        return akka.dispatch.Futures.failed(failure);
    }

    @Override
    public void deleteData(YangInstanceIdentifier path) {
        LOG.debug("Tx {} deleteData called path = {}", getIdentifier(), path);
    }

    @Override
    public void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        LOG.debug("Tx {} mergeData called path = {}", getIdentifier(), path);
    }

    @Override
    public void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        LOG.debug("Tx {} writeData called path = {}", getIdentifier(), path);
    }

    @Override
    public void readData(final YangInstanceIdentifier path, SettableFuture<Optional<NormalizedNode<?, ?>>> proxyFuture) {
        LOG.debug("Tx {} readData called path = {}", getIdentifier(), path);

        final Throwable t;
        if (failure instanceof NoShardLeaderException) {
            t = new DataStoreUnavailableException(failure.getMessage(), failure);
        } else {
            t = failure;
        }
        proxyFuture.setException(new ReadFailedException("Error reading data for path " + path, t));
    }

    @Override
    public void dataExists(YangInstanceIdentifier path, SettableFuture<Boolean> proxyFuture) {
        LOG.debug("Tx {} dataExists called path = {}", getIdentifier(), path);
        proxyFuture.setException(new ReadFailedException("Error checking exists for path " + path, failure));
    }
}
