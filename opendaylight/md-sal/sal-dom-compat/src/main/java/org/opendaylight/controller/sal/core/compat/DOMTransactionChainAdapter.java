/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import static java.util.Objects.requireNonNull;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ForwardingObject;
import java.util.function.Function;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;

@Deprecated
public class DOMTransactionChainAdapter extends ForwardingObject
        implements org.opendaylight.mdsal.dom.api.DOMTransactionChain, TransactionChainListener {
    private final Cache<AsyncTransaction<?, ?>, DOMDataTreeTransaction> transactions = CacheBuilder.newBuilder()
            .weakKeys().weakValues().build();

    private final DOMTransactionChainListener listener;
    private final DOMTransactionChain delegate;

    public DOMTransactionChainAdapter(final DOMTransactionChainListener listener,
            final Function<TransactionChainListener, DOMTransactionChain> function) {
        this.listener = requireNonNull(listener);
        this.delegate = function.apply(this);
    }

    @Override
    public void close() {
        delegate().close();
        transactions.invalidateAll();
    }

    @Override
    public DOMDataTreeReadTransaction newReadOnlyTransaction() {
        final DOMDataReadOnlyTransaction tx = delegate.newReadOnlyTransaction();
        return track(tx, new DOMDataTreeReadTransactionAdapter(tx) {
            @Override
            public void close() {
                untrack(delegate());
                super.close();
            }
        });
    }

    @Override
    public DOMDataTreeWriteTransaction newWriteOnlyTransaction() {
        final DOMDataWriteTransaction tx = delegate.newWriteOnlyTransaction();
        return track(tx, new DOMDataTreeWriteTransactionAdapter(tx) {
            @Override
            public boolean cancel() {
                untrack(delegate());
                return super.cancel();
            }
        });
    }

    @Override
    public DOMDataTreeReadWriteTransaction newReadWriteTransaction() {
        final DOMDataReadWriteTransaction tx = delegate.newReadWriteTransaction();
        return track(tx, new DOMDataTreeReadWriteTransactionAdapter(tx) {
            @Override
            public boolean cancel() {
                untrack(delegate());
                return super.cancel();
            }

            @Override
            public void close() {
                untrack(delegate());
                super.close();
            }
        });
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
            final Throwable cause) {
        listener.onTransactionChainFailed(this, null, cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        listener.onTransactionChainSuccessful(this);
    }

    @Override
    protected DOMTransactionChain delegate() {
        return delegate;
    }

    private <T extends DOMDataTreeTransaction> T track(final AsyncTransaction<?, ?> controllerTx, final T mdsalTx) {
        transactions.put(controllerTx, mdsalTx);
        return mdsalTx;
    }

    void untrack(final AsyncTransaction<?, ?> controllerTx) {
        transactions.invalidate(controllerTx);
    }
}
