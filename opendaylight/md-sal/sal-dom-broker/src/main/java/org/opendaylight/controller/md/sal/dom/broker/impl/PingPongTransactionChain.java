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
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.dom.spi.ForwardingDOMDataReadWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(PingPongTransactionChain.class);
    private final DOMTransactionChain delegate;

    @GuardedBy("this")
    private PingPongTransaction bufferTransaction;
    @GuardedBy("this")
    private PingPongTransaction inflightTransaction;
    @GuardedBy("this")
    private boolean haveLocked;
    @GuardedBy("this")
    private boolean failed;

    PingPongTransactionChain(final DOMDataBroker broker, final TransactionChainListener listener) {
        this.delegate = broker.createTransactionChain(new TransactionChainListener() {
            @Override
            public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction, final Throwable cause) {
                LOG.debug("Delegate chain {} reported failure in {}", chain, transaction, cause);

                final DOMDataReadWriteTransaction frontend;
                if (inflightTransaction == null) {
                    LOG.warn("Transaction chain {} failed with no pending transactions", chain);
                    frontend = null;
                } else {
                    frontend = inflightTransaction.getFrontendTransaction();
                }

                listener.onTransactionChainFailed(PingPongTransactionChain.this, frontend , cause);
                delegateFailed();
            }

            @Override
            public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
                listener.onTransactionChainSuccessful(PingPongTransactionChain.this);
            }
        });
    }

    private synchronized void delegateFailed() {
        failed = true;
        if (!haveLocked) {
            processBuffer();
        }
    }

    private synchronized PingPongTransaction allocateTransaction() {
        Preconditions.checkState(!haveLocked, "Attempted to start a transaction while a previous one is still outstanding");
        Preconditions.checkState(!failed, "Attempted to use a failed chain");

        if (bufferTransaction == null) {
            bufferTransaction = new PingPongTransaction(delegate.newReadWriteTransaction());
        }

        haveLocked = true;
        return bufferTransaction;
    }

    @GuardedBy("this")
    private void processBuffer() {
        final PingPongTransaction tx = bufferTransaction;

        if (tx != null) {
            if (failed) {
                LOG.debug("Cancelling transaction {}", tx);
                tx.getTransaction().cancel();
                bufferTransaction = null;
                return;
            }

            LOG.debug("Submitting transaction {}", tx);
            final CheckedFuture<Void, ?> f = tx.getTransaction().submit();
            bufferTransaction = null;
            inflightTransaction = tx;

            Futures.addCallback(f, new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    transactionSuccessful(tx, result);
                }

                @Override
                public void onFailure(final Throwable t) {
                    transactionFailed(tx, t);
                }
            });
        }
    }

    private void transactionSuccessful(final PingPongTransaction tx, final Void result) {
        LOG.debug("Transaction {} completed successfully", tx);

        synchronized (this) {
            Preconditions.checkState(inflightTransaction == tx, "Successful transaction %s while %s was submitted", tx, inflightTransaction);
            inflightTransaction = null;

            if (!haveLocked) {
                processBuffer();
            }
        }

        // Can run unsynchronized
        tx.onSuccess(result);
    }

    private void transactionFailed(final PingPongTransaction tx, final Throwable t) {
        LOG.debug("Transaction {} failed", tx, t);

        synchronized (this) {
            Preconditions.checkState(inflightTransaction == tx, "Failed transaction %s while %s was submitted", tx, inflightTransaction);
            inflightTransaction = null;
        }

        tx.onFailure(t);
    }

    private synchronized void readyTransaction(final PingPongTransaction tx) {
        Preconditions.checkState(haveLocked, "Attempted to submit transaction while it is not outstanding");
        Preconditions.checkState(bufferTransaction == tx, "Attempted to submit transaction %s while we have %s", tx, bufferTransaction);

        haveLocked = false;
        LOG.debug("Transaction {} unlocked", bufferTransaction);

        if (inflightTransaction == null) {
            processBuffer();
        }
    }

    @Override
    public synchronized void close() {
        Preconditions.checkState(!haveLocked, "Attempted to close chain while a transaction is outstanding");
        processBuffer();
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
        final DOMDataReadWriteTransaction ret = new ForwardingDOMDataReadWriteTransaction() {
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

        tx.recordFrontendTransaction(ret);
        return ret;
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return newReadWriteTransaction();
    }
}
