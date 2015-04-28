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
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.util.concurrent.MappingCheckedFuture;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * A single sub-transaction of a {@link TransactionProxy}, backed by a {@link TransactionFutureCallback},
 * which does most of the real work.
 */
final class RemoteTransactionComponent extends AbstractTransactionComponent {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteTransactionComponent.class);
    private final TransactionFutureCallback txFutureCallback;
    private final TransactionType transactionType;
    private final RemoteTransactionContext context;

    RemoteTransactionComponent(final TransactionFutureCallback txFutureCallback, final RemoteTransactionContext context, final TransactionType type) {
        this.transactionType = Preconditions.checkNotNull(type);
        this.context = Preconditions.checkNotNull(context);
        this.txFutureCallback = Preconditions.checkNotNull(txFutureCallback);
    }

    private Object getIdentifier() {
        return context.getIdentifier();
    }

    @Override
    CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {
        Preconditions.checkState(transactionType != TransactionType.WRITE_ONLY,
                "Exists operation on write-only transaction is not allowed");

        LOG.debug("Tx {} exists {}", getIdentifier(), path);

        context.throttleOperation();

        final SettableFuture<Boolean> proxyFuture = SettableFuture.create();
        txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.dataExists(path, proxyFuture);
            }
        });

        return MappingCheckedFuture.create(proxyFuture, ReadFailedException.MAPPER);
    }

    @Override
    CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final YangInstanceIdentifier path) {
        Preconditions.checkState(transactionType != TransactionType.WRITE_ONLY,
                "Read operation on write-only transaction is not allowed");

        LOG.debug("Tx {} read {}", getIdentifier(), path);

        context.throttleOperation();

        final SettableFuture<Optional<NormalizedNode<?, ?>>> proxyFuture = SettableFuture.create();
        txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.readData(path, proxyFuture);
            }
        });

        return MappingCheckedFuture.create(proxyFuture, ReadFailedException.MAPPER);
    }

    @Override
    void delete(final YangInstanceIdentifier path) {
        LOG.debug("Tx {} delete {}", getIdentifier(), path);

        context.throttleOperation();

        txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.deleteData(path);
            }
        });
    }

    @Override
    void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        LOG.debug("Tx {} merge {}", getIdentifier(), path);

        context.throttleOperation();

        txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.mergeData(path, data);
            }
        });
    }

    @Override
    void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        LOG.debug("Tx {} write {}", getIdentifier(), path);

        context.throttleOperation();

        txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.writeData(path, data);
            }
        });
    }

    final void close() {
        txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.closeTransaction();
            }
        });
    }

    @Override
    final Future<ActorSelection> coordinatedCommit() {
        final TransactionContext transactionContext = txFutureCallback.getTransactionContext();
        if (transactionContext != null) {
            // avoid the creation of a promise and a TransactionOperation
            return transactionContext.readyTransaction();
        }

        final Promise<ActorSelection> promise = akka.dispatch.Futures.promise();
        txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                promise.completeWith(transactionContext.readyTransaction());
            }
        });

        return promise.future();
    }

    @Override
    final AbstractThreePhaseCommitCohort<Object> uncoordinatedCommit(final ActorContext actorContext) {
        final OperationCallback.Reference operationCallbackRef =
                new OperationCallback.Reference(OperationCallback.NO_OP_CALLBACK);

        final TransactionContext transactionContext = txFutureCallback.getTransactionContext();
        final Future future;
        if (transactionContext == null) {
            final Promise promise = akka.dispatch.Futures.promise();
            txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
                @Override
                public void invoke(TransactionContext transactionContext) {
                    promise.completeWith(getReadyOrDirectCommitFuture(actorContext, transactionContext, operationCallbackRef));
                }
            });
            future = promise.future();
        } else {
            // avoid the creation of a promise and a TransactionOperation
            future = getReadyOrDirectCommitFuture(actorContext, transactionContext, operationCallbackRef);
        }

        return new SingleCommitCohortProxy(actorContext, future, getIdentifier().toString(), operationCallbackRef);
    }

    private static Future<?> getReadyOrDirectCommitFuture(final ActorContext actorContext, TransactionContext transactionContext,
            OperationCallback.Reference operationCallbackRef) {
        if (transactionContext.supportsDirectCommit()) {
            TransactionRateLimitingCallback rateLimitingCallback = new TransactionRateLimitingCallback(actorContext);
            operationCallbackRef.set(rateLimitingCallback);
            rateLimitingCallback.run();
            return transactionContext.directCommit();
        } else {
            return transactionContext.readyTransaction();
        }
    }
}
