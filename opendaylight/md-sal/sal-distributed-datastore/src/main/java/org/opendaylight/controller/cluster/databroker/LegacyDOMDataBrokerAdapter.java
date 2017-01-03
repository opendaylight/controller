/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingObject;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataStoreUnavailableException;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.impl.service.AbstractDataTransaction;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;
import org.opendaylight.yangtools.util.concurrent.MappingCheckedFuture;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Adapter between the legacy controller DOMDataBroker and the mdsal DOMDataBroker APIs.
 *
 * @author Thomas Pantelis
 */
public class LegacyDOMDataBrokerAdapter extends ForwardingObject implements DOMDataBroker {
    private static final ExceptionMapper<TransactionCommitFailedException> SUBMIT_EX_MAPPER =
            new ExceptionMapper<TransactionCommitFailedException>("submit", TransactionCommitFailedException.class) {
        @Override
        protected TransactionCommitFailedException newWithCause(String message, Throwable cause) {
            if (cause instanceof org.opendaylight.mdsal.common.api.OptimisticLockFailedException) {
                return new OptimisticLockFailedException(cause.getMessage(), cause.getCause());
            } else if (cause instanceof org.opendaylight.mdsal.common.api.TransactionCommitFailedException) {
                Throwable rootCause = cause.getCause();
                if (rootCause instanceof org.opendaylight.mdsal.common.api.DataStoreUnavailableException) {
                    rootCause = new DataStoreUnavailableException(rootCause.getMessage(), rootCause.getCause());
                }

                return new TransactionCommitFailedException(cause.getMessage(), rootCause);
            }

            return new TransactionCommitFailedException(message, cause);
        }
    };

    private static final ExceptionMapper<ReadFailedException> READ_EX_MAPPER =
            new ExceptionMapper<ReadFailedException>("read", ReadFailedException.class) {
        @Override
        protected ReadFailedException newWithCause(String message, Throwable cause) {
            if (cause instanceof org.opendaylight.mdsal.common.api.ReadFailedException) {
                return new ReadFailedException(cause.getMessage(), cause.getCause());
            }

            return new ReadFailedException(message, cause);
        }
    };

    private final AbstractDOMBroker delegate;
    private final Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> extensions;

    public LegacyDOMDataBrokerAdapter(AbstractDOMBroker delegate) {
        this.delegate = delegate;

        Map<Class<? extends org.opendaylight.mdsal.dom.api.DOMDataBrokerExtension>,
            org.opendaylight.mdsal.dom.api.DOMDataBrokerExtension> delegateExtensions =
                delegate.getSupportedExtensions();

        Builder<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> extBuilder = ImmutableMap.builder();
        final org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService delegateTreeChangeService =
                (org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService) delegateExtensions.get(
                        org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService.class);
        if (delegateTreeChangeService != null) {
            extBuilder.put(DOMDataTreeChangeService.class, new DOMDataTreeChangeService() {
                @Override
                public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerDataTreeChangeListener(
                        DOMDataTreeIdentifier treeId, final L listener) {
                    final org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener delegateListener;
                    if (listener instanceof ClusteredDOMDataTreeChangeListener) {
                        delegateListener = (org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener)
                            changes -> listener.onDataTreeChanged(changes);
                    } else {
                        delegateListener = changes -> listener.onDataTreeChanged(changes);
                    }

                    final ListenerRegistration<org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener> reg =
                        delegateTreeChangeService.registerDataTreeChangeListener(
                            new org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier(convert(treeId.getDatastoreType()),
                                    treeId.getRootIdentifier()), delegateListener);

                    return new ListenerRegistration<L>() {
                        @Override
                        public L getInstance() {
                            return listener;
                        }

                        @Override
                        public void close() {
                            reg.close();
                        }
                    };
                }
            });
        }

        final org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry delegateCohortRegistry =
                (org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry) delegateExtensions.get(
                        org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry.class);
        if (delegateCohortRegistry != null) {
            extBuilder.put(DOMDataTreeCommitCohortRegistry.class, new DOMDataTreeCommitCohortRegistry() {
                @Override
                public <T extends DOMDataTreeCommitCohort> DOMDataTreeCommitCohortRegistration<T> registerCommitCohort(
                        org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier path, T cohort) {
                    return delegateCohortRegistry.registerCommitCohort(path, cohort);
                }
            });
        }

        extensions = extBuilder.build();
    }

    @Override
    protected AbstractDOMBroker delegate() {
        return delegate;
    }

    @Override
    public Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> getSupportedExtensions() {
        return extensions;
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return new DOMDataReadOnlyTransactionAdapter(delegate().newReadOnlyTransaction());
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return new DOMDataTransactionAdapter(delegate().newReadWriteTransaction());
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return new DOMDataTransactionAdapter(delegate().newWriteOnlyTransaction());
    }

    @Override
    public DOMTransactionChain createTransactionChain(final TransactionChainListener listener) {
        AtomicReference<DOMTransactionChain> legacyChain = new AtomicReference<>();
        org.opendaylight.mdsal.common.api.TransactionChainListener delegateListener =
                new org.opendaylight.mdsal.common.api.TransactionChainListener() {
            @SuppressWarnings("rawtypes")
            @Override
            public void onTransactionChainFailed(final org.opendaylight.mdsal.common.api.TransactionChain<?, ?> chain,
                    final org.opendaylight.mdsal.common.api.AsyncTransaction<?, ?> transaction, final Throwable cause) {
                listener.onTransactionChainFailed(legacyChain.get(),
                        (AsyncTransaction) () -> transaction.getIdentifier(),
                            cause instanceof Exception ? SUBMIT_EX_MAPPER.apply((Exception)cause) : cause);
            }

            @Override
            public void onTransactionChainSuccessful(org.opendaylight.mdsal.common.api.TransactionChain<?, ?> chain) {
                listener.onTransactionChainSuccessful(legacyChain.get());
            }
        };

        final org.opendaylight.mdsal.dom.api.DOMTransactionChain delegateChain =
                delegate().createTransactionChain(delegateListener);
        legacyChain.set(new DOMTransactionChain() {
            @Override
            public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
                return new DOMDataReadOnlyTransactionAdapter(delegateChain.newReadOnlyTransaction());
            }

            @Override
            public DOMDataReadWriteTransaction newReadWriteTransaction() {
                return new DOMDataTransactionAdapter(delegateChain.newReadWriteTransaction());
            }

            @Override
            public DOMDataWriteTransaction newWriteOnlyTransaction() {
                return new DOMDataTransactionAdapter(delegateChain.newWriteOnlyTransaction());
            }

            @Override
            public void close() {
                delegateChain.close();
            }
        });

        return legacyChain.get();
    }

    @Override
    public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(final LogicalDatastoreType store,
            final YangInstanceIdentifier path, final DOMDataChangeListener listener,
            final DataChangeScope triggeringScope) {
        org.opendaylight.mdsal.dom.spi.store.DOMStore potentialStore = delegate().getTxFactories().get(convert(store));
        checkState(potentialStore != null, "Requested logical data store is not available.");
        checkState(potentialStore instanceof DistributedDataStoreInterface,
                "Requested logical data store does not support DataChangeListener.");
        return ((DistributedDataStoreInterface)potentialStore).registerChangeListener(path, listener, triggeringScope);
    }

    private static org.opendaylight.mdsal.common.api.LogicalDatastoreType convert(LogicalDatastoreType datastoreType) {
        return org.opendaylight.mdsal.common.api.LogicalDatastoreType.valueOf(datastoreType.name());
    }

    private static class DOMDataTransactionAdapter implements DOMDataReadWriteTransaction {
        private final DOMDataTreeReadTransaction readDelegate;
        private final DOMDataTreeWriteTransaction writeDelegate;
        private final Object identifier;

        DOMDataTransactionAdapter(@Nonnull DOMDataTreeReadTransaction readDelegate) {
            this.readDelegate = Preconditions.checkNotNull(readDelegate);
            this.identifier = readDelegate.getIdentifier();
            this.writeDelegate = null;
        }

        DOMDataTransactionAdapter(@Nonnull DOMDataTreeWriteTransaction writeDelegate) {
            this.writeDelegate = Preconditions.checkNotNull(writeDelegate);
            this.identifier = writeDelegate.getIdentifier();
            this.readDelegate = null;
        }

        DOMDataTransactionAdapter(@Nonnull DOMDataTreeReadWriteTransaction rwDelegate) {
            this.readDelegate = Preconditions.checkNotNull(rwDelegate);
            this.writeDelegate = rwDelegate;
            this.identifier = readDelegate.getIdentifier();
        }

        DOMDataTreeReadTransaction readDelegate() {
            return readDelegate;
        }

        DOMDataTreeWriteTransaction writeDelegate() {
            return writeDelegate;
        }

        @Override
        public Object getIdentifier() {
            return identifier;
        }

        @Override
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(LogicalDatastoreType store,
                YangInstanceIdentifier path) {
            return MappingCheckedFuture.create(readDelegate().read(convert(store), path), READ_EX_MAPPER);
        }

        @Override
        public CheckedFuture<Boolean, ReadFailedException> exists(LogicalDatastoreType store,
                YangInstanceIdentifier path) {
            return MappingCheckedFuture.create(readDelegate().exists(convert(store), path), READ_EX_MAPPER);
        }

        @Override
        public void delete(LogicalDatastoreType store, YangInstanceIdentifier path) {
            writeDelegate().delete(convert(store), path);
        }

        @Override
        public void put(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
            writeDelegate().put(convert(store), path, data);
        }

        @Override
        public void merge(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
            writeDelegate().merge(convert(store), path, data);
        }

        @Override
        public boolean cancel() {
            return writeDelegate().cancel();
        }

        @Override
        public CheckedFuture<Void, TransactionCommitFailedException> submit() {
            return MappingCheckedFuture.create(writeDelegate().submit(), SUBMIT_EX_MAPPER);
        }

        @Override
        public ListenableFuture<RpcResult<TransactionStatus>> commit() {
            return AbstractDataTransaction.convertToLegacyCommitFuture(submit());
        }
    }

    private static class DOMDataReadOnlyTransactionAdapter implements DOMDataReadOnlyTransaction {
        private final DOMDataTransactionAdapter adapter;

        DOMDataReadOnlyTransactionAdapter(DOMDataTreeReadTransaction delegateTx) {
            adapter = new DOMDataTransactionAdapter(delegateTx);
        }

        @Override
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(LogicalDatastoreType store,
                YangInstanceIdentifier path) {
            return adapter.read(store, path);
        }

        @Override
        public CheckedFuture<Boolean, ReadFailedException> exists(LogicalDatastoreType store,
                YangInstanceIdentifier path) {
            return adapter.exists(store, path);
        }

        @Override
        public Object getIdentifier() {
            return adapter.getIdentifier();
        }

        @Override
        public void close() {
            adapter.readDelegate().close();
        }
    }
}
