/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.concepts.Delegator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BindingTransactionChainAdapter implements BindingTransactionChain,
        Delegator<org.opendaylight.mdsal.binding.api.BindingTransactionChain> {

    private static final Logger LOG = LoggerFactory.getLogger(BindingTransactionChainAdapter.class);

    private final org.opendaylight.mdsal.binding.api.BindingTransactionChain delegate;
    private final DelegateChainListener delegateListener;
    private final TransactionChainListener bindingListener;

    BindingTransactionChainAdapter(final org.opendaylight.mdsal.binding.api.DataBroker delegateDataBroker,
            final TransactionChainListener listener) {
        this.delegateListener = new DelegateChainListener();
        this.bindingListener = listener;
        this.delegate = requireNonNull(delegateDataBroker).createTransactionChain(delegateListener);
    }

    @Override
    public org.opendaylight.mdsal.binding.api.BindingTransactionChain getDelegate() {
        return delegate;
    }

    @Override
    public ReadOnlyTransaction newReadOnlyTransaction() {
        return new BindingReadTransactionAdapter(delegate.newReadOnlyTransaction());
    }

    @Override
    public ReadWriteTransaction newReadWriteTransaction() {
        return new BindingReadWriteTransactionAdapter(delegate.newReadWriteTransaction()) {
            @Override
            public FluentFuture<? extends CommitInfo> commit() {
                return listenForFailure(this, super.commit());
            }
        };
    }

    @Override
    public WriteTransaction newWriteOnlyTransaction() {
        return new BindingWriteTransactionAdapter<org.opendaylight.mdsal.binding.api.WriteTransaction>(
                delegate.newWriteOnlyTransaction()) {
            @Override
            public FluentFuture<? extends CommitInfo> commit() {
                return listenForFailure(this, super.commit());
            }
        };
    }

    private FluentFuture<? extends CommitInfo> listenForFailure(
            final WriteTransaction tx, final FluentFuture<? extends CommitInfo> future) {
        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onFailure(final Throwable ex) {
                failTransactionChain(tx,ex);
            }

            @Override
            public void onSuccess(final CommitInfo result) {
                // Intentional NOOP
            }
        }, MoreExecutors.directExecutor());

        return future;
    }

    private void failTransactionChain(final WriteTransaction tx, final Throwable ex) {
        /*
         *  We assume correct state change for underlying transaction chain, so we are not changing any of our
         *  internal state to mark that we failed.
         */
        this.bindingListener.onTransactionChainFailed(this, tx, ex);
    }

    @Override
    public void close() {
        delegate.close();
    }

    private final class DelegateChainListener implements org.opendaylight.mdsal.common.api.TransactionChainListener {

        @Override
        public void onTransactionChainFailed(org.opendaylight.mdsal.common.api.TransactionChain<?, ?> chain,
                org.opendaylight.mdsal.common.api.AsyncTransaction<?, ?> transaction, Throwable cause) {
            Preconditions.checkState(delegate.equals(chain),
                    "Illegal state - listener for %s was invoked for incorrect chain %s.", delegate, chain);
            /*
             * Intentional NOOP since we are also listening on each transaction future for failure,
             * in order to have a reference to the binding transaction instance seen by the client of this chain),
             * instead of the delegate transaction which is known only internally to this chain.
             */
            LOG.debug("Transaction chain {} failed. Failed delegate Transaction {}", this, transaction, cause);
        }

        @Override
        public void onTransactionChainSuccessful(org.opendaylight.mdsal.common.api.TransactionChain<?, ?> chain) {
            Preconditions.checkState(delegate.equals(chain),
                    "Illegal state - listener for %s was invoked for incorrect chain %s.", delegate, chain);
            bindingListener.onTransactionChainSuccessful(BindingTransactionChainAdapter.this);
        }
    }
}
