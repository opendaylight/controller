/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.List;
import javassist.ClassPool;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.impl.AbstractForwardedDataBroker;
import org.opendaylight.controller.md.sal.binding.impl.AbstractForwardedTransaction;
import org.opendaylight.controller.md.sal.binding.impl.AbstractWriteTransaction;
import org.opendaylight.controller.md.sal.binding.impl.BindingNotificationPublishServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingNotificationServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.impl.LazyDataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter;
import org.opendaylight.controller.md.sal.dom.broker.impl.SerializedDOMDataBroker;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.binding.test.util.MockSchemaService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMNotificationPublishServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMNotificationServiceAdapter;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public abstract class AbstractDataBrokerTestCustomizer {

    private DOMDataBroker domDataBroker;
    private org.opendaylight.controller.md.sal.dom.api.DOMDataBroker mdsalDomDataBroker;
    private final DOMNotificationRouter domNotificationRouter;
    private final org.opendaylight.mdsal.dom.broker.DOMNotificationRouter mdsalDomNotificationRouter;
    private final MockSchemaService schemaService;
    private ImmutableMap<LogicalDatastoreType, DOMStore> datastores;
    private final BindingToNormalizedNodeCodec bindingToNormalized;

    public ImmutableMap<LogicalDatastoreType, DOMStore> createDatastores() {
        return ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                .put(LogicalDatastoreType.OPERATIONAL, createOperationalDatastore())
                .put(LogicalDatastoreType.CONFIGURATION,createConfigurationDatastore())
                .build();
    }

    public AbstractDataBrokerTestCustomizer() {
        this.schemaService = new MockSchemaService();
        final ClassPool pool = ClassPool.getDefault();
        final DataObjectSerializerGenerator generator = StreamWriterGenerator.create(JavassistUtils.forClassPool(pool));
        final BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(generator);
        final GeneratedClassLoadingStrategy loading = GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy();
        this.bindingToNormalized = new BindingToNormalizedNodeCodec(loading, codecRegistry);
        this.schemaService.registerSchemaContextListener(this.bindingToNormalized);

        this.mdsalDomNotificationRouter = org.opendaylight.mdsal.dom.broker.DOMNotificationRouter.create(16);
        this.domNotificationRouter = DOMNotificationRouter.create(mdsalDomNotificationRouter,
                mdsalDomNotificationRouter, mdsalDomNotificationRouter);
    }

    public DOMStore createConfigurationDatastore() {
        final InMemoryDOMDataStore store = new InMemoryDOMDataStore("CFG", getDataTreeChangeListenerExecutor());
        this.schemaService.registerSchemaContextListener(store);
        return store;
    }

    public DOMStore createOperationalDatastore() {
        final InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", getDataTreeChangeListenerExecutor());
        this.schemaService.registerSchemaContextListener(store);
        return store;
    }

    public DOMDataBroker createDOMDataBroker() {
        return new SerializedDOMDataBroker(getDatastores(), getCommitCoordinatorExecutor());
    }

    public NotificationService createNotificationService() {
        return new BindingNotificationServiceAdapter(new BindingDOMNotificationServiceAdapter(
                this.mdsalDomNotificationRouter, this.bindingToNormalized.getCodecRegistry()));
    }

    public NotificationPublishService createNotificationPublishService() {
        return new BindingNotificationPublishServiceAdapter(new BindingDOMNotificationPublishServiceAdapter(
                this.mdsalDomNotificationRouter, this.bindingToNormalized));
    }

    public abstract ListeningExecutorService getCommitCoordinatorExecutor();

    public ListeningExecutorService getDataTreeChangeListenerExecutor() {
        return MoreExecutors.newDirectExecutorService();
    }

    public DataBroker createDataBroker() {
        return new LegacyBindingDOMDataBrokerAdapter(getDOMDataBroker(), this.bindingToNormalized);
    }

    public BindingToNormalizedNodeCodec getBindingToNormalized() {
        return this.bindingToNormalized;
    }

    public SchemaService getSchemaService() {
        return this.schemaService;
    }

    public DOMDataBroker getDOMDataBroker() {
        if (this.domDataBroker == null) {
            this.domDataBroker = createDOMDataBroker();
        }
        return this.domDataBroker;
    }

    private synchronized ImmutableMap<LogicalDatastoreType, DOMStore> getDatastores() {
        if (this.datastores == null) {
            this.datastores = createDatastores();
        }
        return this.datastores;
    }

    public void updateSchema(final SchemaContext ctx) {
        this.schemaService.changeSchema(ctx);
    }

    public DOMNotificationRouter getDomNotificationRouter() {
        return this.domNotificationRouter;
    }

    private static class LegacyBindingDOMDataBrokerAdapter extends AbstractForwardedDataBroker implements DataBroker {
        private final DOMDataTreeChangeService dataTreeChangeService;

        LegacyBindingDOMDataBrokerAdapter(DOMDataBroker domDataBroker, BindingToNormalizedNodeCodec codec) {
            super(domDataBroker, codec);
            dataTreeChangeService = requireNonNull((DOMDataTreeChangeService) domDataBroker
                    .getSupportedExtensions().get(DOMDataTreeChangeService.class));
        }

        @Override

        public ReadOnlyTransaction newReadOnlyTransaction() {
            return new LegacyBindingDOMReadTransactionAdapter(getDelegate().newReadOnlyTransaction(), getCodec());
        }

        @Override
        public ReadWriteTransaction newReadWriteTransaction() {
            return new LegacyBindingDOMReadWriteTransactionAdapter(getDelegate().newReadWriteTransaction(), getCodec());
        }

        @Override
        public WriteTransaction newWriteOnlyTransaction() {
            return new LegacyBindingDOMWriteTransactionAdapter<>(getDelegate().newWriteOnlyTransaction(), getCodec());
        }

        @Override
        public BindingTransactionChain createTransactionChain(final TransactionChainListener listener) {
            return new LegacyBindingDOMTransactionChainAdapter(getDelegate(), getCodec(), listener);
        }

        @Override
        public String toString() {
            return "BindingDOMDataBrokerAdapter for " + getDelegate();
        }

        @Override
        public <T extends DataObject, L extends DataTreeChangeListener<T>> ListenerRegistration<L>
                registerDataTreeChangeListener(final DataTreeIdentifier<T> treeId, final L listener) {
            final DOMDataTreeIdentifier domIdentifier = toDomTreeIdentifier(treeId);

            final DOMDataTreeChangeListener domListener = changes -> listener.onDataTreeChanged(
                    LazyDataTreeModification.from(getCodec(), changes, treeId.getDatastoreType()));

            final ListenerRegistration<?> domReg =
                    dataTreeChangeService.registerDataTreeChangeListener(domIdentifier, domListener);
            return new AbstractListenerRegistration<L>(listener) {
                @Override
                protected void removeRegistration() {
                    domReg.close();
                }
            };
        }

        private DOMDataTreeIdentifier toDomTreeIdentifier(final DataTreeIdentifier<?> treeId) {
            final YangInstanceIdentifier domPath =
                    getCodec().toYangInstanceIdentifierBlocking(treeId.getRootIdentifier());
            return new DOMDataTreeIdentifier(treeId.getDatastoreType(), domPath);
        }
    }

    private static class LegacyBindingDOMWriteTransactionAdapter<T extends DOMDataWriteTransaction> extends
            AbstractWriteTransaction<T> implements WriteTransaction {

        LegacyBindingDOMWriteTransactionAdapter(final T delegateTx, final BindingToNormalizedNodeCodec codec) {
            super(delegateTx, codec);
        }

        @Override
        public <U extends DataObject> void put(final LogicalDatastoreType store, final InstanceIdentifier<U> path,
                final U data) {
            put(store, path, data, false);
        }

        @Override
        public <D extends DataObject> void merge(final LogicalDatastoreType store, final InstanceIdentifier<D> path,
                final D data) {
            merge(store, path, data, false);
        }


        @Override
        protected void ensureParentsByMerge(final LogicalDatastoreType store,
                final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier normalizedPath,
                final InstanceIdentifier<?> path) {
            List<PathArgument> currentArguments = new ArrayList<>();
            DataNormalizationOperation<?> currentOp = getCodec().getDataNormalizer().getRootOperation();
            for (PathArgument currentArg : normalizedPath.getPathArguments()) {
                try {
                    currentOp = currentOp.getChild(currentArg);
                } catch (DataNormalizationException e) {
                    throw new IllegalArgumentException(String.format("Invalid child encountered in path %s", path), e);
                }
                currentArguments.add(currentArg);
                YangInstanceIdentifier currentPath = YangInstanceIdentifier.create(
                        currentArguments);

                getDelegate().merge(store, currentPath, currentOp.createDefault(currentArg));
            }
        }

        @Override
        public void delete(final LogicalDatastoreType store, final InstanceIdentifier<?> path) {
            doDelete(store, path);
        }

        @Override
        public FluentFuture<? extends CommitInfo> commit() {
            return doCommit();
        }

        @Override
        public boolean cancel() {
            return doCancel();
        }
    }

    private static class LegacyBindingDOMReadWriteTransactionAdapter extends
            LegacyBindingDOMWriteTransactionAdapter<DOMDataReadWriteTransaction> implements ReadWriteTransaction {

        protected LegacyBindingDOMReadWriteTransactionAdapter(final DOMDataReadWriteTransaction delegate,
                final BindingToNormalizedNodeCodec codec) {
            super(delegate, codec);
        }

        @Override
        public <T extends DataObject> CheckedFuture<Optional<T>,ReadFailedException> read(
                final LogicalDatastoreType store, final InstanceIdentifier<T> path) {
            return doRead(getDelegate(), store, path);
        }
    }

    private static class LegacyBindingDOMReadTransactionAdapter
            extends AbstractForwardedTransaction<DOMDataReadOnlyTransaction> implements ReadOnlyTransaction {
        protected LegacyBindingDOMReadTransactionAdapter(final DOMDataReadOnlyTransaction delegate,
                final BindingToNormalizedNodeCodec codec) {
            super(delegate, codec);
        }

        @Override
        public <T extends DataObject> CheckedFuture<Optional<T>,ReadFailedException> read(
                final LogicalDatastoreType store, final InstanceIdentifier<T> path) {
            return doRead(getDelegate(),store, path);
        }

        @Override
        public void close() {
            getDelegate().close();
        }
    }

    private static class LegacyBindingDOMTransactionChainAdapter implements BindingTransactionChain {
        private final DOMTransactionChain delegate;
        private final BindingToNormalizedNodeCodec codec;
        private final TransactionChainListener domListener;
        private final TransactionChainListener bindingListener;

        LegacyBindingDOMTransactionChainAdapter(final DOMDataBroker chainFactory,
                final BindingToNormalizedNodeCodec codec, final TransactionChainListener listener) {
            this.bindingListener = listener;

            this.domListener = new TransactionChainListener() {
                @Override
                public void onTransactionChainFailed(TransactionChain<?, ?> chain, AsyncTransaction<?, ?> transaction,
                        Throwable cause) {
                }

                @Override
                public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
                    bindingListener.onTransactionChainSuccessful(LegacyBindingDOMTransactionChainAdapter.this);
                }
            };

            this.delegate = chainFactory.createTransactionChain(domListener);
            this.codec = codec;
        }

        @Override
        public ReadOnlyTransaction newReadOnlyTransaction() {
            return new LegacyBindingDOMReadTransactionAdapter(delegate.newReadOnlyTransaction(), codec);
        }

        @Override
        public ReadWriteTransaction newReadWriteTransaction() {
            final DOMDataReadWriteTransaction delegateTx = delegate.newReadWriteTransaction();
            return new LegacyBindingDOMReadWriteTransactionAdapter(delegateTx, codec) {
                @Override
                public FluentFuture<? extends CommitInfo> commit() {
                    return listenForFailure(this, super.commit());
                }
            };
        }

        @Override
        public WriteTransaction newWriteOnlyTransaction() {
            final DOMDataWriteTransaction delegateTx = delegate.newWriteOnlyTransaction();
            return new LegacyBindingDOMWriteTransactionAdapter<DOMDataWriteTransaction>(delegateTx, codec) {
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
                    bindingListener.onTransactionChainFailed(LegacyBindingDOMTransactionChainAdapter.this, tx, ex);
                }

                @Override
                public void onSuccess(final CommitInfo result) {
                    // Intentionally NOOP
                }
            }, MoreExecutors.directExecutor());

            return future;
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
