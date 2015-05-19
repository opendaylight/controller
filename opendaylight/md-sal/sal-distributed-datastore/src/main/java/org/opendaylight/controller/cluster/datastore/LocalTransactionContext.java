/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import scala.concurrent.Future;

/**
 * Processes front-end transaction operations locally before being committed to the destination shard.
 * Instances of this class are used when the destination shard is local to the caller.
 *
 * @author Thomas Pantelis
 */
final class LocalTransactionContext extends AbstractTransactionContext {
    private final DOMStoreReadWriteTransaction delegate;

    LocalTransactionContext(final DOMStoreReadWriteTransaction delegate, final OperationLimiter limiter) {
        super(limiter);
        this.delegate = Preconditions.checkNotNull(delegate);
    }

    @Override
    public void writeData(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        delegate.write(path, data);
        releaseOperation();
    }

    @Override
    public void mergeData(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        delegate.merge(path, data);
        releaseOperation();
    }

    @Override
    public void deleteData(final YangInstanceIdentifier path) {
        delegate.delete(path);
        releaseOperation();
    }

    @Override
    public void readData(final YangInstanceIdentifier path, final SettableFuture<Optional<NormalizedNode<?, ?>>> proxyFuture) {

        Futures.addCallback(delegate.read(path), new FutureCallback<Optional<NormalizedNode<?, ?>>>() {
            @Override
            public void onSuccess(final Optional<NormalizedNode<?, ?>> result) {
                proxyFuture.set(result);
                releaseOperation();
            }

            @Override
            public void onFailure(final Throwable t) {
                proxyFuture.setException(t);
                releaseOperation();
            }
        });
    }

    @Override
    public void dataExists(final YangInstanceIdentifier path, final SettableFuture<Boolean> proxyFuture) {
        Futures.addCallback(delegate.exists(path), new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(final Boolean result) {
                proxyFuture.set(result);
                releaseOperation();
            }

            @Override
            public void onFailure(final Throwable t) {
                proxyFuture.setException(t);
                releaseOperation();
            }
        });
    }

    private LocalThreePhaseCommitCohort ready() {
        acquireOperation();
        return (LocalThreePhaseCommitCohort) delegate.ready();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T extends Future> T completeOperation(final ActorContext actorContext, final T operationFuture) {
        operationFuture.onComplete(getLimiter(), actorContext.getClientDispatcher());
        return operationFuture;
    }

    @Override
    public Future<ActorSelection> readyTransaction() {
        final LocalThreePhaseCommitCohort cohort = ready();
        return completeOperation(cohort.getActorContext(), cohort.initiateCoordinatedCommit());
    }

    @Override
    public Future<Object> directCommit() {
        final LocalThreePhaseCommitCohort cohort = ready();
        return completeOperation(cohort.getActorContext(), cohort.initiateDirectCommit());
    }

    @Override
    public boolean supportsDirectCommit() {
        return true;
    }

    @Override
    public void closeTransaction() {
        delegate.close();
        releaseOperation();
    }
}
