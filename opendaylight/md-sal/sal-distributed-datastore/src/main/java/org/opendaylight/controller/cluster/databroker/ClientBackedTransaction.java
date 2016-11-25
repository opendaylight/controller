/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import com.google.common.base.FinalizablePhantomReference;
import com.google.common.base.FinalizableReferenceQueue;
import com.google.common.base.Preconditions;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.databroker.actors.dds.AbstractClientHandle;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.sal.core.spi.data.AbstractDOMStoreTransaction;
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
    private static final class Finalizer extends FinalizablePhantomReference<ClientBackedTransaction<?>> {
        private static final FinalizableReferenceQueue QUEUE = new FinalizableReferenceQueue();
        private static final Set<Finalizer> FINALIZERS = ConcurrentHashMap.newKeySet();
        private static final Logger LOG = LoggerFactory.getLogger(Finalizer.class);

        private final AbstractClientHandle<?> transaction;

        private Finalizer(final ClientBackedTransaction<?> referent, final AbstractClientHandle<?> transaction) {
            super(referent, QUEUE);
            this.transaction = Preconditions.checkNotNull(transaction);
        }

        static @Nonnull <T extends AbstractClientHandle<?>> T recordTransaction(
                @Nonnull final ClientBackedTransaction<T> referent, @Nonnull final T transaction) {
            FINALIZERS.add(new Finalizer(referent, transaction));
            return transaction;
        }

        @Override
        public void finalizeReferent() {
            FINALIZERS.remove(this);
            if (transaction.abort()) {
                LOG.warn("Aborted orphan transaction {}", transaction.getIdentifier());
            }
        }
    }

    private final T delegate;

    ClientBackedTransaction(final T delegate) {
        super(delegate.getIdentifier());
        this.delegate = Finalizer.recordTransaction(this, delegate);
    }

    final T delegate() {
        return delegate;
    }

    @Override
    public void close() {
        delegate.abort();
    }
}
