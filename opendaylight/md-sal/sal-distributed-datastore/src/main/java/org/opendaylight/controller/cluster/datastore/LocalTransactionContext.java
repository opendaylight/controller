/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.AbstractRead;
import org.opendaylight.controller.cluster.datastore.modification.AbstractModification;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import scala.concurrent.Future;

/**
 * Processes front-end transaction operations locally before being committed to the destination shard.
 * Instances of this class are used when the destination shard is local to the caller.
 *
 * @author Thomas Pantelis
 */
abstract class LocalTransactionContext extends AbstractTransactionContext {
    private final DOMStoreTransaction txDelegate;
    private final LocalTransactionReadySupport readySupport;
    private Exception operationError;

    LocalTransactionContext(DOMStoreTransaction txDelegate, TransactionIdentifier identifier,
            LocalTransactionReadySupport readySupport) {
        super(identifier);
        this.txDelegate = Preconditions.checkNotNull(txDelegate);
        this.readySupport = readySupport;
    }

    protected abstract DOMStoreWriteTransaction getWriteDelegate();

    protected abstract DOMStoreReadTransaction getReadDelegate();

    @Override
    public void executeModification(AbstractModification modification) {
        incrementModificationCount();
        if(operationError == null) {
            try {
                modification.apply(getWriteDelegate());
            } catch (Exception e) {
                operationError = e;
            }
        }
    }

    @Override
    public <T> void executeRead(AbstractRead<T> readCmd, final SettableFuture<T> proxyFuture) {
        Futures.addCallback(readCmd.apply(getReadDelegate()), new FutureCallback<T>() {
            @Override
            public void onSuccess(final T result) {
                proxyFuture.set(result);
            }

            @Override
            public void onFailure(final Throwable t) {
                proxyFuture.setException(t);
            }
        });
    }

    private LocalThreePhaseCommitCohort ready() {
        logModificationCount();
        return readySupport.onTransactionReady(getWriteDelegate(), operationError);
    }

    @Override
    public Future<ActorSelection> readyTransaction() {
        final LocalThreePhaseCommitCohort cohort = ready();
        return cohort.initiateCoordinatedCommit();
    }

    @Override
    public Future<Object> directCommit() {
        final LocalThreePhaseCommitCohort cohort = ready();
        return cohort.initiateDirectCommit();
    }

    @Override
    public void closeTransaction() {
        txDelegate.close();
    }
}
