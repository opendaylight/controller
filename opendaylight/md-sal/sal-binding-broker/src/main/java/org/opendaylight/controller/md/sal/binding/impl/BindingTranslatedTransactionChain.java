/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.yangtools.concepts.Delegator;

import com.google.common.base.Preconditions;

class BindingTranslatedTransactionChain implements BindingTransactionChain, Delegator<DOMTransactionChain> {

    private final DOMTransactionChain delegate;

    @GuardedBy("this")
    private final Map<AsyncTransaction<?, ?>, AsyncTransaction<?, ?>> delegateTxToBindingTx = new WeakHashMap<>();
    private final BindingToNormalizedNodeCodec codec;

    public BindingTranslatedTransactionChain(final DOMDataBroker chainFactory,
            final BindingToNormalizedNodeCodec codec, final TransactionChainListener listener) {
        Preconditions.checkNotNull(chainFactory, "DOM Transaction chain factory must not be null");
        this.delegate = chainFactory.createTransactionChain(new ListenerInvoker(listener));
        this.codec = codec;
    }

    @Override
    public DOMTransactionChain getDelegate() {
        return delegate;
    }

    @Override
    public ReadOnlyTransaction newReadOnlyTransaction() {
        DOMDataReadOnlyTransaction delegateTx = delegate.newReadOnlyTransaction();
        ReadOnlyTransaction bindingTx = new BindingDataReadTransactionImpl(delegateTx, codec);
        putDelegateToBinding(delegateTx, bindingTx);
        return bindingTx;
    }

    @Override
    public ReadWriteTransaction newReadWriteTransaction() {
        DOMDataReadWriteTransaction delegateTx = delegate.newReadWriteTransaction();
        ReadWriteTransaction bindingTx = new BindingDataReadWriteTransactionImpl(delegateTx, codec);
        putDelegateToBinding(delegateTx, bindingTx);
        return bindingTx;
    }

    @Override
    public WriteTransaction newWriteOnlyTransaction() {
        DOMDataWriteTransaction delegateTx = delegate.newWriteOnlyTransaction();
        WriteTransaction bindingTx = new BindingDataWriteTransactionImpl<>(delegateTx, codec);
        putDelegateToBinding(delegateTx, bindingTx);
        return bindingTx;
    }

    @Override
    public void close() {
        delegate.close();
    }

    private synchronized void putDelegateToBinding(final AsyncTransaction<?, ?> domTx,
            final AsyncTransaction<?, ?> bindingTx) {
        final Object previous = delegateTxToBindingTx.put(domTx, bindingTx);
        Preconditions.checkState(previous == null, "DOM Transaction %s has already associated binding transation %s",domTx,previous);
    }

    private synchronized AsyncTransaction<?, ?> getBindingTransaction(final AsyncTransaction<?, ?> transaction) {
        return delegateTxToBindingTx.get(transaction);
    }

    private final class ListenerInvoker implements TransactionChainListener {

        private final TransactionChainListener listener;

        public ListenerInvoker(final TransactionChainListener listener) {
            this.listener = Preconditions.checkNotNull(listener, "Listener must not be null.");
        }

        @Override
        public void onTransactionChainFailed(final TransactionChain<?, ?> chain,
                final AsyncTransaction<?, ?> transaction, final Throwable cause) {
            Preconditions.checkState(delegate.equals(chain),
                    "Illegal state - listener for %s was invoked for incorrect chain %s.", delegate, chain);
            AsyncTransaction<?, ?> bindingTx = getBindingTransaction(transaction);
            listener.onTransactionChainFailed(chain, bindingTx, cause);
        }

        @Override
        public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
            Preconditions.checkState(delegate.equals(chain),
                    "Illegal state - listener for %s was invoked for incorrect chain %s.", delegate, chain);
            listener.onTransactionChainSuccessful(BindingTranslatedTransactionChain.this);
        }
    }

}
