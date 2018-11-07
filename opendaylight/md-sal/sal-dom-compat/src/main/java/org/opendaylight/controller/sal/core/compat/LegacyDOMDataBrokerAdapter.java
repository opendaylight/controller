/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ForwardingObject;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap.Builder;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.MappingCheckedFuture;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataStoreUnavailableException;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainClosedException;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Adapter between the legacy controller API-based DOMDataBroker and the mdsal API-based DOMDataBroker.
 *
 * @author Thomas Pantelis
 */
public class LegacyDOMDataBrokerAdapter extends ForwardingObject implements DOMDataBroker {
    private static final ExceptionMapper<TransactionCommitFailedException> COMMIT_EX_MAPPER =
            new ExceptionMapper<TransactionCommitFailedException>("commit", TransactionCommitFailedException.class) {
        @Override
        protected TransactionCommitFailedException newWithCause(final String message, final Throwable cause) {
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

    private final org.opendaylight.mdsal.dom.api.DOMDataBroker delegate;
    private final ClassToInstanceMap<DOMDataBrokerExtension> extensions;

    public LegacyDOMDataBrokerAdapter(final org.opendaylight.mdsal.dom.api.DOMDataBroker delegate) {
        this.delegate = delegate;

        ClassToInstanceMap<org.opendaylight.mdsal.dom.api.DOMDataBrokerExtension> delegateExtensions =
                delegate.getExtensions();

        Builder<DOMDataBrokerExtension> extBuilder = ImmutableClassToInstanceMap.builder();
        final org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService delegateTreeChangeService =
                (org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService) delegateExtensions.get(
                        org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService.class);
        if (delegateTreeChangeService != null) {
            extBuilder.put(DOMDataTreeChangeService.class, new DOMDataTreeChangeService() {
                @Override
                public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerDataTreeChangeListener(
                        final DOMDataTreeIdentifier treeId, final L listener) {
                    final org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener delegateListener;
                    if (listener instanceof ClusteredDOMDataTreeChangeListener) {
                        delegateListener = (org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener)
                            listener::onDataTreeChanged;
                    } else {
                        delegateListener = listener::onDataTreeChanged;
                    }

                    final ListenerRegistration<org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener> reg =
                        delegateTreeChangeService.registerDataTreeChangeListener(treeId.toMdsal(), delegateListener);

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
            extBuilder.put(DOMDataTreeCommitCohortRegistry.class, delegateCohortRegistry::registerCommitCohort);
        }

        extensions = extBuilder.build();
    }

    @Override
    protected org.opendaylight.mdsal.dom.api.DOMDataBroker delegate() {
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
        DOMTransactionChainListener delegateListener =
                new DOMTransactionChainListener() {
            @Override
            @SuppressWarnings("rawtypes")
            public void onTransactionChainFailed(final org.opendaylight.mdsal.dom.api.DOMTransactionChain chain,
                    final DOMDataTreeTransaction transaction, final Throwable cause) {
                listener.onTransactionChainFailed(legacyChain.get(),
                    (AsyncTransaction) () -> transaction.getIdentifier(),
                        cause instanceof Exception ? COMMIT_EX_MAPPER.apply((Exception)cause) : cause);
            }

            @Override
            public void onTransactionChainSuccessful(final org.opendaylight.mdsal.dom.api.DOMTransactionChain chain) {
                listener.onTransactionChainSuccessful(legacyChain.get());
            }
        };

        final org.opendaylight.mdsal.dom.api.DOMTransactionChain delegateChain =
                delegate().createTransactionChain(delegateListener);
        legacyChain.set(new DOMTransactionChain() {
            @Override
            public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
                return new DOMDataReadOnlyTransactionAdapter(wrapException(delegateChain::newReadOnlyTransaction));
            }

            @Override
            public DOMDataReadWriteTransaction newReadWriteTransaction() {
                return new DOMDataTransactionAdapter(wrapException(delegateChain::newReadWriteTransaction));
            }

            @Override
            public DOMDataWriteTransaction newWriteOnlyTransaction() {
                return new DOMDataTransactionAdapter(wrapException(delegateChain::newWriteOnlyTransaction));
            }

            @Override
            public void close() {
                delegateChain.close();
            }
        });

        return legacyChain.get();
    }

    static <T> T wrapException(final Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (DOMTransactionChainClosedException e) {
            throw new TransactionChainClosedException("Transaction chain already closed", e);
        }
    }

    private static class DOMDataTransactionAdapter implements DOMDataReadWriteTransaction {
        private final DOMDataTreeReadTransaction readDelegate;
        private final DOMDataTreeWriteTransaction writeDelegate;
        private final Object identifier;

        DOMDataTransactionAdapter(@Nonnull final DOMDataTreeReadTransaction readDelegate) {
            this.readDelegate = Preconditions.checkNotNull(readDelegate);
            this.identifier = readDelegate.getIdentifier();
            this.writeDelegate = null;
        }

        DOMDataTransactionAdapter(@Nonnull final DOMDataTreeWriteTransaction writeDelegate) {
            this.writeDelegate = Preconditions.checkNotNull(writeDelegate);
            this.identifier = writeDelegate.getIdentifier();
            this.readDelegate = null;
        }

        DOMDataTransactionAdapter(@Nonnull final DOMDataTreeReadWriteTransaction rwDelegate) {
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
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final LogicalDatastoreType store,
                final YangInstanceIdentifier path) {
            return MappingCheckedFuture.create(readDelegate().read(store.toMdsal(), path).transform(
                Optional::fromJavaUtil, MoreExecutors.directExecutor()), ReadFailedExceptionAdapter.INSTANCE);
        }

        @Override
        public CheckedFuture<Boolean, ReadFailedException> exists(final LogicalDatastoreType store,
                final YangInstanceIdentifier path) {
            return MappingCheckedFuture.create(readDelegate().exists(store.toMdsal(), path),
                    ReadFailedExceptionAdapter.INSTANCE);
        }

        @Override
        public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
            writeDelegate().delete(store.toMdsal(), path);
        }

        @Override
        public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                final NormalizedNode<?, ?> data) {
            writeDelegate().put(store.toMdsal(), path, data);
        }

        @Override
        public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                final NormalizedNode<?, ?> data) {
            writeDelegate().merge(store.toMdsal(), path, data);
        }

        @Override
        public boolean cancel() {
            return writeDelegate().cancel();
        }

        @Override
        public FluentFuture<? extends CommitInfo> commit() {
            final SettableFuture<CommitInfo> resultFuture = SettableFuture.create();
            writeDelegate().commit().addCallback(new FutureCallback<CommitInfo>() {
                @Override
                public void onSuccess(final CommitInfo result) {
                    resultFuture.set(result);
                }

                @Override
                public void onFailure(final Throwable ex) {
                    if (ex instanceof Exception) {
                        resultFuture.setException(COMMIT_EX_MAPPER.apply((Exception)ex));
                    } else {
                        resultFuture.setException(ex);
                    }
                }
            }, MoreExecutors.directExecutor());

            return resultFuture;
        }
    }

    private static class DOMDataReadOnlyTransactionAdapter implements DOMDataReadOnlyTransaction {
        private final DOMDataTransactionAdapter adapter;

        DOMDataReadOnlyTransactionAdapter(final DOMDataTreeReadTransaction delegateTx) {
            adapter = new DOMDataTransactionAdapter(delegateTx);
        }

        @Override
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final LogicalDatastoreType store,
                final YangInstanceIdentifier path) {
            return adapter.read(store, path);
        }

        @Override
        public CheckedFuture<Boolean, ReadFailedException> exists(final LogicalDatastoreType store,
                final YangInstanceIdentifier path) {
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
