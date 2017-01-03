/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.compat;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;
import org.opendaylight.yangtools.util.concurrent.MappingCheckedFuture;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Adapter between the legacy controller API-based DOMStore and the mdsal API-based DOMStore.
 *
 * @author Thomas Pantelis
 */
public class LegacyDOMStoreAdapter extends ForwardingObject implements DOMStore, AutoCloseable {
    public static final ExceptionMapper<ReadFailedException> READ_EX_MAPPER =
            new ExceptionMapper<ReadFailedException>("read", ReadFailedException.class) {
        @Override
        protected ReadFailedException newWithCause(String message, Throwable cause) {
            if (cause instanceof org.opendaylight.mdsal.common.api.ReadFailedException) {
                return new ReadFailedException(cause.getMessage(), cause.getCause());
            }

            return new ReadFailedException(message, cause);
        }
    };

    private final DistributedDataStoreInterface delegate;

    public LegacyDOMStoreAdapter(DistributedDataStoreInterface delegate) {
        this.delegate = delegate;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new DOMStoreTransactionAdapter(delegate().newReadOnlyTransaction());
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new DOMStoreTransactionAdapter(delegate().newWriteOnlyTransaction());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new DOMStoreTransactionAdapter(delegate().newReadWriteTransaction());
    }

    @Override
    public <L extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L>
            registerChangeListener(YangInstanceIdentifier path, L listener, DataChangeScope scope) {
        return delegate().registerChangeListener(path, listener, scope);
    }

    @Override
    public DOMStoreTransactionChain createTransactionChain() {
        final org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain delegateChain =
                delegate().createTransactionChain();
        return new DOMStoreTransactionChain() {
            @Override
            public DOMStoreReadTransaction newReadOnlyTransaction() {
                return new DOMStoreTransactionAdapter(delegateChain.newReadOnlyTransaction());
            }

            @Override
            public DOMStoreWriteTransaction newWriteOnlyTransaction() {
                return new DOMStoreTransactionAdapter(delegateChain.newWriteOnlyTransaction());
            }

            @Override
            public DOMStoreReadWriteTransaction newReadWriteTransaction() {
                return new DOMStoreTransactionAdapter(delegateChain.newReadWriteTransaction());
            }

            @Override
            public void close() {
                delegateChain.close();
            }
        };
    }

    @Override
    public void close() {
    }

    @Override
    protected DistributedDataStoreInterface delegate() {
        return delegate;
    }

    private static class DOMStoreTransactionAdapter implements DOMStoreReadWriteTransaction {
        private final org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction readDelegate;
        private final org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction writeDelegate;
        private final Object identifier;

        DOMStoreTransactionAdapter(@Nonnull org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction readDelegate) {
            this.readDelegate = Preconditions.checkNotNull(readDelegate);
            this.identifier = readDelegate.getIdentifier();
            this.writeDelegate = null;
        }

        DOMStoreTransactionAdapter(
                @Nonnull org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction writeDelegate) {
            this.writeDelegate = Preconditions.checkNotNull(writeDelegate);
            this.identifier = writeDelegate.getIdentifier();
            this.readDelegate = null;
        }

        DOMStoreTransactionAdapter(
                @Nonnull org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction rwDelegate) {
            this.readDelegate = Preconditions.checkNotNull(rwDelegate);
            this.writeDelegate = rwDelegate;
            this.identifier = readDelegate.getIdentifier();
        }

        @Override
        public Object getIdentifier() {
            return identifier;
        }

        @Override
        public void close() {
            if (writeDelegate != null) {
                writeDelegate.close();
            } else {
                readDelegate.close();
            }
        }

        @Override
        public void write(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
            writeDelegate.write(path, data);
        }

        @Override
        public void merge(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
            writeDelegate.merge(path, data);
        }

        @Override
        public void delete(YangInstanceIdentifier path) {
            writeDelegate.delete(path);
        }

        @Override
        public DOMStoreThreePhaseCommitCohort ready() {
            final org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort cohort = writeDelegate.ready();
            return new DOMStoreThreePhaseCommitCohort() {
                @Override
                public ListenableFuture<Boolean> canCommit() {
                    return cohort.canCommit();
                }

                @Override
                public ListenableFuture<Void> preCommit() {
                    return cohort.preCommit();
                }

                @Override
                public ListenableFuture<Void> commit() {
                    return cohort.commit();
                }

                @Override
                public ListenableFuture<Void> abort() {
                    return cohort.abort();
                }
            };
        }

        @Override
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(YangInstanceIdentifier path) {
            return MappingCheckedFuture.create(readDelegate.read(path), READ_EX_MAPPER);
        }

        @Override
        public CheckedFuture<Boolean, ReadFailedException> exists(YangInstanceIdentifier path) {
            return MappingCheckedFuture.create(readDelegate.exists(path), READ_EX_MAPPER);
        }
    }
}
