/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import java.lang.ref.Cleaner;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.databroker.actors.dds.AbstractClientHandle;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.mdsal.dom.spi.store.AbstractDOMStoreTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link DOMStoreTransaction} backed by a {@link ClientTransaction}. It guards against user-level
 * leaks by maintaining a phantom reference on the backing transaction, which will ensure that the transaction will
 * be aborted, if it is not otherwise closed, just before this object is garbage-collected.
 *
 * @author Robert Varga
 */
abstract class ClientBackedTransaction<T extends AbstractClientHandle<?>> extends
        AbstractDOMStoreTransaction<TransactionIdentifier> {
    private static final class Finalizer {
        private static final Logger LOG = LoggerFactory.getLogger(Finalizer.class);

        private static final Cleaner CLEANER = Cleaner.create();

        static <T extends AbstractClientHandle<?>> @NonNull T recordTransaction(
                final @NonNull ClientBackedTransaction<T> referent, final @NonNull T transaction,
                final @Nullable Throwable allocationContext) {
            CLEANER.register(referent, new Cleanup(transaction, allocationContext));
            return transaction;
        }

        private static class Cleanup implements Runnable {
            private final AbstractClientHandle<?> transaction;
            private final Throwable allocationContext;

            Cleanup(AbstractClientHandle<?> transaction, Throwable allocationContext) {
                this.transaction = transaction;
                this.allocationContext = allocationContext;
            }

            @Override
            public void run() {
                if (transaction.abort()) {
                    LOG.info("Aborted orphan transaction {}", transaction, allocationContext);
                }
            }
        }
    }

    private final T delegate;

    ClientBackedTransaction(final T delegate, final Throwable allocationContext) {
        super(delegate.getIdentifier());
        this.delegate = Finalizer.recordTransaction(this, delegate, allocationContext);
    }

    final T delegate() {
        return delegate;
    }

    @Override
    public void close() {
        delegate.abort();
    }
}
