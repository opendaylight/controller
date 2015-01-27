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
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.concurrent.Semaphore;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

final class NoOpTransactionContext extends AbstractTransactionContext {
    private static final Logger LOG = LoggerFactory.getLogger(NoOpTransactionContext.class);

    private final Throwable failure;
    private final Semaphore operationLimiter;

    public NoOpTransactionContext(Throwable failure, TransactionIdentifier identifier, Semaphore operationLimiter) {
        super(identifier);
        this.failure = failure;
        this.operationLimiter = operationLimiter;
    }

    @Override
    public void closeTransaction() {
        LOG.debug("NoOpTransactionContext {} closeTransaction called", identifier);
    }

    @Override
    public Future<ActorSelection> readyTransaction() {
        LOG.debug("Tx {} readyTransaction called", identifier);
        operationLimiter.release();
        return akka.dispatch.Futures.failed(failure);
    }

    @Override
    public void deleteData(YangInstanceIdentifier path) {
        LOG.debug("Tx {} deleteData called path = {}", identifier, path);
        operationLimiter.release();
    }

    @Override
    public void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        LOG.debug("Tx {} mergeData called path = {}", identifier, path);
        operationLimiter.release();
    }

    @Override
    public void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        LOG.debug("Tx {} writeData called path = {}", identifier, path);
        operationLimiter.release();
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readData(
            YangInstanceIdentifier path) {
        LOG.debug("Tx {} readData called path = {}", identifier, path);
        operationLimiter.release();
        return Futures.immediateFailedCheckedFuture(new ReadFailedException(
                "Error reading data for path " + path, failure));
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> dataExists(
            YangInstanceIdentifier path) {
        LOG.debug("Tx {} dataExists called path = {}", identifier, path);
        operationLimiter.release();
        return Futures.immediateFailedCheckedFuture(new ReadFailedException(
                "Error checking exists for path " + path, failure));
    }
}