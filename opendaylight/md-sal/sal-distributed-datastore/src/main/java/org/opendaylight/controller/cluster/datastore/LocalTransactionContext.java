/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorSelection;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.AbstractRead;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import scala.concurrent.Future;

/**
 * Processes front-end transaction operations locally before being committed to the destination shard.
 * Instances of this class are used when the destination shard is local to the caller.
 *
 * @author Thomas Pantelis
 */
abstract class LocalTransactionContext extends TransactionContext {
    private final DOMStoreTransaction txDelegate;
    private final LocalTransactionReadySupport readySupport;
    private Exception operationError;

    LocalTransactionContext(final DOMStoreTransaction txDelegate, final TransactionIdentifier identifier,
            final LocalTransactionReadySupport readySupport) {
        super(identifier);
        this.txDelegate = requireNonNull(txDelegate);
        this.readySupport = readySupport;
    }

    abstract DOMStoreWriteTransaction getWriteDelegate();

    abstract DOMStoreReadTransaction getReadDelegate();

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void executeModification(final Consumer<DOMStoreWriteTransaction> consumer) {
        incrementModificationCount();
        if (operationError == null) {
            try {
                consumer.accept(getWriteDelegate());
            } catch (Exception e) {
                operationError = e;
            }
        }
    }

    @Override
    void executeDelete(final YangInstanceIdentifier path, final Boolean havePermit) {
        executeModification(transaction -> transaction.delete(path));
    }

    @Override
    void executeMerge(final YangInstanceIdentifier path, final NormalizedNode data, final Boolean havePermit) {
        executeModification(transaction -> transaction.merge(path, data));
    }

    @Override
    void executeWrite(final YangInstanceIdentifier path, final NormalizedNode data, final Boolean havePermit) {
        executeModification(transaction -> transaction.write(path, data));
    }

    @Override
    <T> void executeRead(final AbstractRead<T> readCmd, final SettableFuture<T> proxyFuture,
            final Boolean havePermit) {
        Futures.addCallback(readCmd.apply(getReadDelegate()), new FutureCallback<T>() {
            @Override
            public void onSuccess(final T result) {
                proxyFuture.set(result);
            }

            @Override
            public void onFailure(final Throwable failure) {
                proxyFuture.setException(failure instanceof Exception
                        ? ReadFailedException.MAPPER.apply((Exception) failure) : failure);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    Future<ActorSelection> readyTransaction(final Boolean havePermit,
            final Optional<SortedSet<String>> participatingShardNames) {
        final LocalThreePhaseCommitCohort cohort = ready();
        return cohort.initiateCoordinatedCommit(participatingShardNames);
    }

    @Override
    Future<Object> directCommit(final Boolean havePermit) {
        final LocalThreePhaseCommitCohort cohort = ready();
        return cohort.initiateDirectCommit();
    }

    @Override
    void closeTransaction() {
        txDelegate.close();
    }

    private LocalThreePhaseCommitCohort ready() {
        logModificationCount();
        return readySupport.onTransactionReady(getWriteDelegate(), operationError);
    }
}
