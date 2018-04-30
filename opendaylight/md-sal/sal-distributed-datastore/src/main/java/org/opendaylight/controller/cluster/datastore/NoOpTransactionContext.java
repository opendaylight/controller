/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import com.google.common.collect.BiMap;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.Executor;
import javax.xml.xpath.XPathException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.messages.AbstractRead;
import org.opendaylight.controller.cluster.datastore.modification.AbstractModification;
import org.opendaylight.mdsal.common.api.DataStoreUnavailableException;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.dom.api.xpath.DOMXPathCallback;
import org.opendaylight.yangtools.concepts.CheckedValue;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

final class NoOpTransactionContext extends AbstractTransactionContext {
    private static final Logger LOG = LoggerFactory.getLogger(NoOpTransactionContext.class);

    private final Throwable failure;

    NoOpTransactionContext(final Throwable failure, final TransactionIdentifier identifier) {
        super(identifier);
        this.failure = failure;
    }

    @Override
    public void closeTransaction() {
        LOG.debug("NoOpTransactionContext {} closeTransaction called", getIdentifier());
    }

    @Override
    public Future<Object> directCommit(final Boolean havePermit) {
        LOG.debug("Tx {} directCommit called, failure: {}", getIdentifier(), failure);
        return akka.dispatch.Futures.failed(failure);
    }

    @Override
    public Future<ActorSelection> readyTransaction(final Boolean havePermit) {
        LOG.debug("Tx {} readyTransaction called, failure: {}", getIdentifier(), failure);
        return akka.dispatch.Futures.failed(failure);
    }

    @Override
    public void executeModification(final AbstractModification modification, final Boolean havePermit) {
        LOG.debug("Tx {} executeModification {} called path = {}", getIdentifier(),
                modification.getClass().getSimpleName(), modification.getPath());
    }

    @Override
    public <T> void executeRead(final AbstractRead<T> readCmd, final SettableFuture<T> proxyFuture,
            final Boolean havePermit) {
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
    public void executeEvaluate(@NonNull final YangInstanceIdentifier path, @NonNull final String xpath,
            @NonNull final BiMap<String, QNameModule> prefixMapping, @NonNull final DOMXPathCallback callback,
            @NonNull final Executor callbackExecutor, @Nullable final Boolean havePermit) {
        LOG.debug("Tx {} executeEvaluate {} called path = {}", getIdentifier(), xpath, path);
        callbackExecutor.execute(() -> callback.accept(CheckedValue.ofException(new XPathException(failure))));
    }
}
