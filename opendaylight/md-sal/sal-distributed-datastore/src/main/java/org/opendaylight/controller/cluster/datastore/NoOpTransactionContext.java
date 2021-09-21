/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Optional;
import java.util.SortedSet;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.messages.AbstractRead;
import org.opendaylight.mdsal.common.api.DataStoreUnavailableException;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

final class NoOpTransactionContext extends TransactionContext {
    private static final Logger LOG = LoggerFactory.getLogger(NoOpTransactionContext.class);

    private final Throwable failure;

    NoOpTransactionContext(final Throwable failure, final TransactionIdentifier identifier) {
        super(identifier);
        this.failure = failure;
    }

    @Override
    void closeTransaction() {
        LOG.debug("NoOpTransactionContext {} closeTransaction called", getIdentifier());
    }

    @Override
    Future<Object> directCommit(final Boolean havePermit) {
        LOG.debug("Tx {} directCommit called, failure", getIdentifier(), failure);
        return akka.dispatch.Futures.failed(failure);
    }

    @Override
    Future<ActorSelection> readyTransaction(final Boolean havePermit,
            final Optional<SortedSet<String>> participatingShardNamess) {
        LOG.debug("Tx {} readyTransaction called, failure", getIdentifier(), failure);
        return akka.dispatch.Futures.failed(failure);
    }

    @Override
    <T> void executeRead(final AbstractRead<T> readCmd, final SettableFuture<T> proxyFuture, final Boolean havePermit) {
        LOG.debug("Tx {} executeRead {} called path = {}", getIdentifier(), readCmd.getClass().getSimpleName(),
                readCmd.getPath());

        final Throwable t;
        if (failure instanceof NoShardLeaderException) {
            t = new DataStoreUnavailableException(failure.getMessage(), failure);
        } else {
            t = failure;
        }
        proxyFuture.setException(new ReadFailedException("Error executeRead " + readCmd.getClass().getSimpleName()
                + " for path " + readCmd.getPath(), t));
    }

    @Override
    void executeDelete(final YangInstanceIdentifier path, final Boolean havePermit) {
        LOG.debug("Tx {} executeDelete called path = {}", getIdentifier(), path);
    }

    @Override
    void executeMerge(final YangInstanceIdentifier path, final NormalizedNode data, final Boolean havePermit) {
        LOG.debug("Tx {} executeMerge called path = {}", getIdentifier(), path);
    }

    @Override
    void executeWrite(final YangInstanceIdentifier path, final NormalizedNode data, final Boolean havePermit) {
        LOG.debug("Tx {} executeWrite called path = {}", getIdentifier(), path);
    }
}
