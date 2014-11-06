/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.dom.spi.ForwardingDOMDataReadWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * An implementation of {@link DOMTransactionChain}, which has a very specific
 * behavior, which some users may find surprising. If keeps the general
 * intent of the contract, but it makes sure there are never more than two
 * transactions allocated at any given time: one of them is being committed,
 * and while that is happening, the other one acts as the scratch pad. Once
 * the committing transaction completes successfully, the scratch transaction
 * is enqueued as soon as it is ready.
 *
 * This mode of operation means that there is no inherent isolation between
 * the front-end transactions and transactions cannot be reasonably cancelled.
 *
 * It furthermore means that the transactions returned by {@link #newReadOnlyTransaction()}
 * counts as an outstanding transaction and the user may not allocate multiple
 * read-only transactions at the same time.
 */
public final class PingPongTransactionChain implements DOMTransactionChain {
    private final DOMTransactionChain delegate;

    @GuardedBy("this")
    private PingPongTransaction reusableTransaction;
    @GuardedBy("this")
    private boolean haveLocked;
    @GuardedBy("this")
    private boolean havePending;

    PingPongTransactionChain(final DOMTransactionChain delegate) {
        this.delegate = Preconditions.checkNotNull(delegate);
    }

    private synchronized PingPongTransaction allocateTransaction() {
        Preconditions.checkState(!haveLocked, "Attempted to start a transaction while a previous one is still outstanding");

        if (reusableTransaction == null) {
            reusableTransaction = new PingPongTransaction(delegate.newReadWriteTransaction());
        }

        haveLocked = true;
        return reusableTransaction;
    }

    @GuardedBy("this")
    private void commitTransaction() {
        final PingPongTransaction tx = reusableTransaction;
        Futures.addCallback(tx.getTransaction().submit(), new FutureCallback<Object>() {
            @Override
            public void onSuccess(final Object result) {
                finishTransaction();
                tx.onSuccess(result);
            }

            @Override
            public void onFailure(final Throwable t) {
                // FIXME: okay, we need to stop the world here...
                finishTransaction();
                tx.onFailure(t);
            }
        });

        reusableTransaction = null;
    }

    private synchronized void finishTransaction() {
        Preconditions.checkState(havePending, "Finishing transaction while none was submitted");

        if (!haveLocked && reusableTransaction != null) {
            commitTransaction();
        } else {
            havePending = false;
        }
    }

    private synchronized void readyTransaction(final PingPongTransaction tx) {
        Preconditions.checkState(haveLocked, "Attempted to submit transaction while it is not outstanding");
        Preconditions.checkState(reusableTransaction == tx, "Attempted to submit transaction %s while we have %s", tx, reusableTransaction);

        if (!havePending) {
            commitTransaction();
            havePending = true;
        }
        haveLocked = false;
    }

    @Override
    public synchronized void close() {
        Preconditions.checkState(!haveLocked, "Attempted to close chain while a transactions is outstanding");

        if (reusableTransaction != null) {
            commitTransaction();
        }
        delegate.close();
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        final PingPongTransaction tx = allocateTransaction();

        return new DOMDataReadOnlyTransaction() {
            @Override
            public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final LogicalDatastoreType store,
                    final YangInstanceIdentifier path) {
                return tx.getTransaction().read(store, path);
            }

            @Override
            public CheckedFuture<Boolean, ReadFailedException> exists(final LogicalDatastoreType store,
                    final YangInstanceIdentifier path) {
                return tx.getTransaction().exists(store, path);
            }

            @Override
            public Object getIdentifier() {
                return tx.getTransaction().getIdentifier();
            }

            @Override
            public void close() {
                readyTransaction(tx);
            }
        };
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        final PingPongTransaction tx = allocateTransaction();

        return new ForwardingDOMDataReadWriteTransaction() {
            @Override
            protected DOMDataReadWriteTransaction delegate() {
                return tx.getTransaction();
            }

            @Override
            public CheckedFuture<Void, TransactionCommitFailedException> submit() {
                readyTransaction(tx);
                return tx.getSubmitFuture();
            }

            @Override
            public ListenableFuture<RpcResult<TransactionStatus>> commit() {
                readyTransaction(tx);
                return tx.getCommitFuture();
            }

            @Override
            public boolean cancel() {
                throw new UnsupportedOperationException("Transaction cancellation is not supported");
            }
        };
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return newReadWriteTransaction();
    }
}
