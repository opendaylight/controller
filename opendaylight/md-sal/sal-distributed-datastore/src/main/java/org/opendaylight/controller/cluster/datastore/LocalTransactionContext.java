/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
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
    private final OperationCompleter completer;

    LocalTransactionContext(TransactionIdentifier identifier, DOMStoreReadWriteTransaction delegate, OperationCompleter completer) {
        super(identifier);
        this.delegate = Preconditions.checkNotNull(delegate);
        this.completer = Preconditions.checkNotNull(completer);
    }

    @Override
    public void writeData(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        completeOperation(new Runnable() {
            @Override
            public void run() {
                delegate.write(path, data);
            }
        });
    }

    @Override
    public void mergeData(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        completeOperation(new Runnable() {
            @Override
            public void run() {
                delegate.merge(path, data);
            }
        });

    }

    @Override
    public void deleteData(final YangInstanceIdentifier path) {
        completeOperation(new Runnable() {
            @Override
            public void run() {
                delegate.delete(path);
            }
        });

    }

    @Override
    public void readData(final YangInstanceIdentifier path, final SettableFuture<Optional<NormalizedNode<?, ?>>> proxyFuture) {
        completeFutureOperation(new Function<Void, CheckedFuture>() {
            @Nullable
            @Override
            public CheckedFuture apply(Void aVoid) {
                return delegate.read(path);
            }
        }, proxyFuture);
    }

    @Override
    public void dataExists(final YangInstanceIdentifier path, final SettableFuture<Boolean> proxyFuture) {
        completeFutureOperation(new Function<Void, CheckedFuture>() {
            @Nullable
            @Override
            public CheckedFuture apply(Void aVoid) {
                return delegate.exists(path);
            }
        }, proxyFuture);
    }

    private void completeOperation(Runnable operation){
        operation.run();
        completer.onComplete(null, null);
    }

    private <R> R completeOperation(Function<Void, R> operation){
        R ret = operation.apply(null);
        completer.onComplete(null, null);
        return ret;
    }


    private <R, F extends CheckedFuture<R, ReadFailedException>> void completeFutureOperation(Function<Void, F> operation, final SettableFuture<R> proxyFuture){
        Futures.addCallback(operation.apply(null), new FutureCallback<R>() {
            @Override
            public void onSuccess(R result) {
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
        return completeOperation(new Function<Void, LocalThreePhaseCommitCohort>() {
            @Nullable
            @Override
            public LocalThreePhaseCommitCohort apply(@Nullable Void aVoid) {
                return (LocalThreePhaseCommitCohort) delegate.ready();
            }
        });
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
