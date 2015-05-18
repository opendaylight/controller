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
    private final OperationCompleter completer;

    LocalTransactionContext(TransactionIdentifier identifier, DOMStoreTransaction txDelegate, OperationCompleter completer) {
        super(identifier);
        this.txDelegate = Preconditions.checkNotNull(txDelegate);
        this.completer = Preconditions.checkNotNull(completer);
    }

    protected abstract DOMStoreWriteTransaction getWriteDelegate();

    protected abstract DOMStoreReadTransaction getReadDelegate();

    @Override
    public void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        getWriteDelegate().write(path, data);
        completer.onComplete(null, null);
    }

    @Override
    public void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        getWriteDelegate().merge(path, data);
        completer.onComplete(null, null);
    }

    @Override
    public void deleteData(YangInstanceIdentifier path) {
        getWriteDelegate().delete(path);
        completer.onComplete(null, null);
    }

    @Override
    public void readData(YangInstanceIdentifier path, final SettableFuture<Optional<NormalizedNode<?, ?>>> proxyFuture) {
        Futures.addCallback(getReadDelegate().read(path), new FutureCallback<Optional<NormalizedNode<?, ?>>>() {
            @Override
            public void onSuccess(Optional<NormalizedNode<?, ?>> result) {
                proxyFuture.set(result);
                completer.onComplete(null, null);
            }

            @Override
            public void onFailure(Throwable t) {
                proxyFuture.setException(t);
                completer.onComplete(null, null);
            }
        });
    }

    @Override
    public void dataExists(YangInstanceIdentifier path, final SettableFuture<Boolean> proxyFuture) {
        Futures.addCallback(getReadDelegate().exists(path), new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                proxyFuture.set(result);
                completer.onComplete(null, null);
            }

            @Override
            public void onFailure(Throwable t) {
                proxyFuture.setException(t);
                completer.onComplete(null, null);
            }
        });
    }

    private LocalThreePhaseCommitCohort ready() {
        LocalThreePhaseCommitCohort ready = (LocalThreePhaseCommitCohort) getWriteDelegate().ready();
        completer.onComplete(null, null);
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
