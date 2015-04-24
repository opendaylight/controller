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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import scala.concurrent.Future;

/**
 * Local TransactionContext that is committed on a remote shard.
 *
 * @author Thomas Pantelis
 */
final class LocalTransactionContext implements TransactionContext {
    private final DOMStoreReadWriteTransaction delegate;
    private final TransactionIdentifier identifier;

    LocalTransactionContext(TransactionIdentifier identifier, DOMStoreReadWriteTransaction delegate) {
        this.delegate = delegate;
        this.identifier = identifier;
    }

    @Override
    public void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        delegate.write(path, data);
    }

    @Override
    public void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        delegate.merge(path, data);
    }

    @Override
    public void deleteData(YangInstanceIdentifier path) {
        delegate.delete(path);
    }

    @Override
    public void readData(YangInstanceIdentifier path, final SettableFuture<Optional<NormalizedNode<?, ?>>> proxyFuture) {
        Futures.addCallback(delegate.read(path), new FutureCallback<Optional<NormalizedNode<?, ?>>>() {
            @Override
            public void onSuccess(Optional<NormalizedNode<?, ?>> result) {
                proxyFuture.set(result);
            }

            @Override
            public void onFailure(Throwable t) {
                proxyFuture.setException(t);
            }
        });
    }

    @Override
    public void dataExists(YangInstanceIdentifier path, final SettableFuture<Boolean> proxyFuture) {
        Futures.addCallback(delegate.exists(path), new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                proxyFuture.set(result);
            }

            @Override
            public void onFailure(Throwable t) {
                proxyFuture.setException(t);
            }
        });
    }

    private LocalThreePhaseCommitCohort ready() {
        return (LocalThreePhaseCommitCohort) delegate.ready();
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
        delegate.close();
    }
}
