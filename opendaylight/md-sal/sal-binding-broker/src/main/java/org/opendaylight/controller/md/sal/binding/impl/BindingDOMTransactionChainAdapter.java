/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.yangtools.concepts.Delegator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BindingDOMTransactionChainAdapter implements BindingTransactionChain, Delegator<DOMTransactionChain> {

    private static final Logger LOG = LoggerFactory.getLogger(BindingDOMTransactionChainAdapter.class);

    private final DOMTransactionChain delegate;
    private final BindingToNormalizedNodeCodec codec;
    private final DelegateChainListener domListener;
    private final TransactionChainListener bindingListener;

    public BindingDOMTransactionChainAdapter(final DOMDataBroker chainFactory,
            final BindingToNormalizedNodeCodec codec, final TransactionChainListener listener) {
        Preconditions.checkNotNull(chainFactory, "DOM Transaction chain factory must not be null");
        this.domListener = new DelegateChainListener();
        this.bindingListener = listener;
        this.delegate = chainFactory.createTransactionChain(domListener);
        this.codec = codec;
    }

    @Override
    public DOMTransactionChain getDelegate() {
        return delegate;
    }

    @Override
    public ReadOnlyTransaction newReadOnlyTransaction() {
        final DOMDataReadOnlyTransaction delegateTx = delegate.newReadOnlyTransaction();
        return new BindingDOMReadTransactionAdapter(delegateTx, codec);
    }

    @Override
    public ReadWriteTransaction newReadWriteTransaction() {
        final DOMDataReadWriteTransaction delegateTx = delegate.newReadWriteTransaction();
        return new BindingDOMReadWriteTransactionAdapter(delegateTx, codec) {

            @Override
            public CheckedFuture<Void, TransactionCommitFailedException> submit() {
                return listenForFailure(this,super.submit());
            }

        };
    }

    @Override
    public WriteTransaction newWriteOnlyTransaction() {
        final DOMDataWriteTransaction delegateTx = delegate.newWriteOnlyTransaction();
        return new BindingDOMWriteTransactionAdapter<DOMDataWriteTransaction>(delegateTx, codec) {

            @Override
            public CheckedFuture<Void,TransactionCommitFailedException> submit() {
                return listenForFailure(this,super.submit());
            }

        };
    }

    private CheckedFuture<Void, TransactionCommitFailedException> listenForFailure(
            final WriteTransaction tx, final CheckedFuture<Void, TransactionCommitFailedException> future) {
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onFailure(final Throwable t) {
                failTransactionChain(tx,t);
            }

            @Override
            public void onSuccess(final Void result) {
                // Intentionally NOOP
            }
        });

        return future;
    }

    private void failTransactionChain(final WriteTransaction tx, final Throwable t) {
        /*
         *  We asume correct state change for underlaying transaction
         *
         * chain, so we are not changing any of our internal state
         * to mark that we failed.
         */
        this.bindingListener.onTransactionChainFailed(this, tx, t);
    }

    @Override
    public void close() {
        delegate.close();
    }

    private final class DelegateChainListener implements TransactionChainListener {

        @Override
        public void onTransactionChainFailed(final TransactionChain<?, ?> chain,
                final AsyncTransaction<?, ?> transaction, final Throwable cause) {
            Preconditions.checkState(delegate.equals(chain),
                    "Illegal state - listener for %s was invoked for incorrect chain %s.", delegate, chain);
            /*
             * Intentionally NOOP, callback for failure, since we
             * are also listening on each transaction future for failure,
             * in order to have reference to Binding Transaction (which was seen by client
             * of this transaction chain), instead of DOM transaction
             * which is known only to this chain, binding transaction implementation
             * and underlying transaction chain.
             *
             */
            LOG.debug("Transaction chain {} failed. Failed DOM Transaction {}",this,transaction,cause);
        }

        @Override
        public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
            Preconditions.checkState(delegate.equals(chain),
                    "Illegal state - listener for %s was invoked for incorrect chain %s.", delegate, chain);
            bindingListener.onTransactionChainSuccessful(BindingDOMTransactionChainAdapter.this);
        }
    }

}
