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
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import scala.concurrent.Future;

/**
 * Processes front-end transaction operations locally before being committed to the destination shard.
 * Instances of this class are used when the destination shard is local to the caller.
 *
 * @author Thomas Pantelis
 */
abstract class LocalTransactionContext extends AbstractTransactionContext {

    private final DOMStoreTransaction txDelegate;
    private final OperationLimiter limiter;

    LocalTransactionContext(TransactionIdentifier identifier, DOMStoreTransaction txDelegate, OperationLimiter limiter) {
        super(identifier);
        this.txDelegate = Preconditions.checkNotNull(txDelegate);
        this.limiter = Preconditions.checkNotNull(limiter);
    }

    protected abstract DOMStoreWriteTransaction getWriteDelegate();

    protected abstract DOMStoreReadTransaction getReadDelegate();

    @Override
    public void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        incrementModificationCount();
        getWriteDelegate().write(path, data);
        limiter.release();
    }

    @Override
    public void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        incrementModificationCount();
        getWriteDelegate().merge(path, data);
        limiter.release();
    }

    @Override
    public void deleteData(YangInstanceIdentifier path) {
        incrementModificationCount();
        getWriteDelegate().delete(path);
        limiter.release();
    }

    @Override
    public void readData(YangInstanceIdentifier path, final SettableFuture<Optional<NormalizedNode<?, ?>>> proxyFuture) {
        Futures.addCallback(getReadDelegate().read(path), new FutureCallback<Optional<NormalizedNode<?, ?>>>() {
            @Override
            public void onSuccess(Optional<NormalizedNode<?, ?>> result) {
                proxyFuture.set(result);
                limiter.release();
            }

            @Override
            public void onFailure(Throwable t) {
                proxyFuture.setException(t);
                limiter.release();
            }
        });
    }

    @Override
    public void dataExists(YangInstanceIdentifier path, final SettableFuture<Boolean> proxyFuture) {
        Futures.addCallback(getReadDelegate().exists(path), new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                proxyFuture.set(result);
                limiter.release();
            }

            @Override
            public void onFailure(Throwable t) {
                proxyFuture.setException(t);
                limiter.release();
            }
        });
    }

    private LocalThreePhaseCommitCohort ready() {
        logModificationCount();
        LocalThreePhaseCommitCohort ready = (LocalThreePhaseCommitCohort) getWriteDelegate().ready();
        limiter.release();
        return ready;
    }

    @Override
    public Future<ActorSelection> readyTransaction() {
        return ready().initiateCoordinatedCommit();
    }

    @Override
    public Future<Object> directCommit() {
        return ready().initiateDirectCommit();
    }

    @Override
    public boolean supportsDirectCommit() {
        return true;
    }

    @Override
    public void closeTransaction() {
        txDelegate.close();
    }
}
