/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package org.opendaylight.controller.sal.dom.broker;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.common.api.RegistrationListener;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandlerRegistration;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.md.sal.common.impl.ListenerRegistry;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.dom.broker.impl.compat.BackwardsCompatibleDataBroker;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.RoutedRpcDefaultImplementation;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.controller.sal.core.api.RpcRoutingContext;
import org.opendaylight.controller.sal.core.api.data.DataChangeListener;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.data.DataValidator;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;
import org.opendaylight.controller.sal.dom.broker.impl.NotificationRouterImpl;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareRpcBroker;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaContextProvider;
import org.opendaylight.controller.sal.dom.broker.spi.NotificationRouter;
import org.opendaylight.controller.sal.dom.broker.util.ProxySchemaContext;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaServiceListener;

public class BackwardsCompatibleMountPoint implements MountProvisionInstance, SchemaContextProvider, SchemaService {

    private final DataProviderService dataReader;
    private final DataReader<InstanceIdentifier,CompositeNode> readWrapper;

    private final InstanceIdentifier mountPath;
    private final NotificationPublishService notificationPublishService;
    private final RpcProvisionRegistry rpcs;

    private final ListenerRegistry<SchemaServiceListener> schemaListenerRegistry = new ListenerRegistry<>();

    private SchemaContext schemaContext;

    public BackwardsCompatibleMountPoint(final InstanceIdentifier path, final DOMMountPointService.DOMMountPointBuilder mountPointBuilder) {
        this.mountPath = Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(mountPointBuilder);

        dataReader = new DataBrokerImpl();
        readWrapper = new ReadWrapper();
        notificationPublishService = new DelgatingNotificationPublishService();
        rpcs = new SchemaAwareRpcBroker(path.toString(), this);

        mountPointBuilder.addService(DOMDataBroker.class, new BackwardsCompatibleDomStore(dataReader, this));
        mountPointBuilder.addService(NotificationPublishService.class, notificationPublishService);
        mountPointBuilder.addService(RpcProvisionRegistry.class, rpcs);

        mountPointBuilder.addInitialSchemaContext(new ProxySchemaContext(this));

        mountPointBuilder.register();
    }

    public BackwardsCompatibleMountPoint(final InstanceIdentifier path, final DOMMountPoint mount) {
        this.mountPath = Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(mount);

        final DOMDataBroker domBroker = getServiceWithCheck(mount, DOMDataBroker.class);

        this.schemaContext = mount.getSchemaContext();

        dataReader = new BackwardsCompatibleDataBroker(domBroker, this);

        // Set schema context to provide it for BackwardsCompatibleDataBroker
        if(schemaContext != null) {
            setSchemaContext(schemaContext);
        }

        readWrapper = new ReadWrapper();

        notificationPublishService = getServiceWithCheck(mount, NotificationPublishService.class);
        rpcs = getServiceWithCheck(mount, RpcProvisionRegistry.class);
    }

    private <T extends DOMService> T getServiceWithCheck(final DOMMountPoint mount, final Class<T> type) {
        final Optional<T> serviceOptional = mount.getService(type);
        Preconditions.checkArgument(serviceOptional.isPresent(), "Service {} has to be set in {}. " +
                "Cannot construct backwards compatible mount wrapper without it", type, mount);
        return serviceOptional.get();
    }

    @Override
    public void addModule(final Module module) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeModule(final Module module) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SchemaContext getSessionContext() {
        return getSchemaContext();
    }

    @Override
    public SchemaContext getGlobalContext() {
        return getSchemaContext();
    }

    @Override
    public ListenerRegistration<SchemaServiceListener> registerSchemaServiceListener(final SchemaServiceListener listener) {
        return schemaListenerRegistry.register(listener);
    }

    @Override
    public void publish(final CompositeNode notification) {
        notificationPublishService.publish(notification);
    }

    @Override
    public ListenerRegistration<NotificationListener> addNotificationListener(final QName notification, final NotificationListener listener) {
        return notificationPublishService.addNotificationListener(notification, listener);
    }

    // TODO Read wrapper is never used ... same in org.opendaylight.controller.sal.dom.broker.MountPointImpl
    public DataReader<InstanceIdentifier, CompositeNode> getReadWrapper() {
        return readWrapper;
    }

    @Override
    public CompositeNode readConfigurationData(final InstanceIdentifier path) {
        return dataReader.readConfigurationData(path);
    }

    @Override
    public CompositeNode readOperationalData(final InstanceIdentifier path) {
        return dataReader.readOperationalData(path);
    }

    @Override
    public Registration registerOperationalReader(
            final InstanceIdentifier path, final DataReader<InstanceIdentifier, CompositeNode> reader) {
        return dataReader.registerOperationalReader(path, reader);
    }

    @Override
    public Registration registerConfigurationReader(
            final InstanceIdentifier path, final DataReader<InstanceIdentifier, CompositeNode> reader) {
        return dataReader.registerConfigurationReader(path, reader);
    }

    @Override
    public RoutedRpcRegistration addRoutedRpcImplementation(final QName rpcType, final RpcImplementation implementation) {
        return rpcs.addRoutedRpcImplementation(rpcType, implementation);
    }

    @Override
    public void setRoutedRpcDefaultDelegate(final RoutedRpcDefaultImplementation defaultImplementation) {
        rpcs.setRoutedRpcDefaultDelegate(defaultImplementation);
    }

    @Override
    public RpcRegistration addRpcImplementation(final QName rpcType, final RpcImplementation implementation)
            throws IllegalArgumentException {
        return rpcs.addRpcImplementation(rpcType, implementation);
    }

    @Override
    public Set<QName> getSupportedRpcs() {
        return rpcs.getSupportedRpcs();
    }

    @Override
    public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(final QName rpc, final CompositeNode input) {
        return rpcs.invokeRpc(rpc, input);
    }

    @Override
    public ListenerRegistration<RpcRegistrationListener> addRpcRegistrationListener(final RpcRegistrationListener listener) {
        return rpcs.addRpcRegistrationListener(listener);
    }

    @Override
    public ListenableFuture<RpcResult<CompositeNode>> rpc(final QName type, final CompositeNode input) {
        return rpcs.invokeRpc(type, input);
    }

    @Override
    public DataModificationTransaction beginTransaction() {
        return dataReader.beginTransaction();
    }

    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(final InstanceIdentifier path,
            final DataChangeListener listener) {
        return dataReader.registerDataChangeListener(path, listener);
    }

    @Override
    public Registration registerCommitHandler(
            final InstanceIdentifier path, final DataCommitHandler<InstanceIdentifier, CompositeNode> commitHandler) {
        return dataReader.registerCommitHandler(path, commitHandler);
    }

    @Override
    public void removeRefresher(final DataStoreIdentifier store, final DataRefresher refresher) {
        // NOOP
    }

    @Override
    public void addRefresher(final DataStoreIdentifier store, final DataRefresher refresher) {
        // NOOP
    }

    @Override
    public void addValidator(final DataStoreIdentifier store, final DataValidator validator) {
        // NOOP
    }
    @Override
    public void removeValidator(final DataStoreIdentifier store, final DataValidator validator) {
        // NOOP
    }

    @Override
    public SchemaContext getSchemaContext() {
        return schemaContext;
    }

    @Override
    public void setSchemaContext(final SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
        for (ListenerRegistration<SchemaServiceListener> schemaServiceListenerListenerRegistration : schemaListenerRegistry.getListeners()) {
            schemaServiceListenerListenerRegistration.getInstance().onGlobalContextUpdated(schemaContext);
        }
    }

    class ReadWrapper implements DataReader<InstanceIdentifier, CompositeNode> {
        private InstanceIdentifier shortenPath(final InstanceIdentifier path) {
            InstanceIdentifier ret = null;
            if(mountPath.contains(path)) {
                final List<PathArgument> newArgs = path.getPath().subList(mountPath.getPath().size(), path.getPath().size());
                ret = InstanceIdentifier.create(newArgs);
            }
            return ret;
        }

        @Override
        public CompositeNode readConfigurationData(final InstanceIdentifier path) {
            final InstanceIdentifier newPath = shortenPath(path);
            if(newPath == null) {
                return null;
            }
            return BackwardsCompatibleMountPoint.this.readConfigurationData(newPath);
        }

        @Override
        public CompositeNode readOperationalData(final InstanceIdentifier path) {
            final InstanceIdentifier newPath = shortenPath(path);
            if(newPath == null) {
                return null;
            }
            return BackwardsCompatibleMountPoint.this.readOperationalData(newPath);
        }
    }

    @Override
    public ListenerRegistration<RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier, CompositeNode>>> registerCommitHandlerListener(
            final RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier, CompositeNode>> commitHandlerListener) {
        return dataReader.registerCommitHandlerListener(commitHandlerListener);
    }

    @Override
    public <L extends RouteChangeListener<RpcRoutingContext, InstanceIdentifier>> ListenerRegistration<L> registerRouteChangeListener(
            final L listener) {
        return rpcs.registerRouteChangeListener(listener);
    }

    @VisibleForTesting
    static final class BackwardsCompatibleDomStore implements DOMDataBroker {
        private final DataProviderService dataReader;
        private final SchemaContextProvider schemaContextProvider;

        public BackwardsCompatibleDomStore(final DataProviderService dataReader, final SchemaContextProvider schemaContextProvider) {
            this.dataReader = dataReader;
            this.schemaContextProvider = schemaContextProvider;
        }

        @Override
        public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
            final DataNormalizer dataNormalizer = new DataNormalizer(schemaContextProvider.getSchemaContext());
            return new BackwardsCompatibleReadTransaction(dataReader, dataNormalizer);
        }

        @Override
        public DOMDataWriteTransaction newWriteOnlyTransaction() {
            final DataNormalizer dataNormalizer = new DataNormalizer(schemaContextProvider.getSchemaContext());
            return new BackwardsCompatibleWriteTransaction(dataReader, dataNormalizer);
        }

        @Override
        public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(final LogicalDatastoreType store, final InstanceIdentifier path, final DOMDataChangeListener listener, final DataChangeScope triggeringScope) {
            throw new UnsupportedOperationException("Register data listener not supported for mount point");
        }

        @Override
        public DOMTransactionChain createTransactionChain(final TransactionChainListener listener) {
            throw new UnsupportedOperationException("Transaction chain not supported for mount point");
        }

        @Override
        public DOMDataReadWriteTransaction newReadWriteTransaction() {
            final DataNormalizer dataNormalizer = new DataNormalizer(schemaContextProvider.getSchemaContext());
            return new BackwardsCompatibleReadWriteTransaction(dataReader, dataNormalizer);
        }

        @VisibleForTesting
        static final class BackwardsCompatibleReadTransaction implements DOMDataReadOnlyTransaction {
            private final DataProviderService dataReader;
            private final DataNormalizer normalizer;

            public BackwardsCompatibleReadTransaction(final DataProviderService dataReader, final DataNormalizer normalizer) {
                this.dataReader = dataReader;
                this.normalizer = normalizer;
            }

            @Override
            public Object getIdentifier() {
                return this;
            }

            @Override
            public void close() {
                // NOOP
            }

            @Override
            public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store, final InstanceIdentifier path) {

                CompositeNode rawData = null;

                switch (store) {
                    case CONFIGURATION: {
                        rawData = dataReader.readConfigurationData(path);
                        break;
                    }
                    case OPERATIONAL: {
                        rawData = dataReader.readOperationalData(path);
                        break;
                    }
                }
                Preconditions.checkNotNull(rawData, "Unable to read %s data on path %s", store, path);

                final Map.Entry<InstanceIdentifier, NormalizedNode<?, ?>> normalized = normalizer.toNormalized(path, rawData);
                final Optional<NormalizedNode<?, ?>> normalizedNodeOptional = Optional.<NormalizedNode<?, ?>>fromNullable(normalized.getValue());
                return com.google.common.util.concurrent.Futures.immediateFuture(normalizedNodeOptional);
            }
        }

        @VisibleForTesting
        static final class BackwardsCompatibleWriteTransaction implements DOMDataWriteTransaction {
            private DataModificationTransaction oldTx;
            private final DataNormalizer dataNormalizer;

            public BackwardsCompatibleWriteTransaction(final DataProviderService dataReader, final DataNormalizer dataNormalizer) {
                this.oldTx = dataReader.beginTransaction();
                this.dataNormalizer = dataNormalizer;
            }

            @Override
            public Object getIdentifier() {
                return this;
            }

            @Override
            public boolean cancel() {
                oldTx = null;
                return true;
            }

            @Override
            public void put(final LogicalDatastoreType store, final InstanceIdentifier path, final NormalizedNode<?, ?> data) {
                final CompositeNode legacyData = dataNormalizer.toLegacy(path, data);
                try {
                    final InstanceIdentifier legacyPath = dataNormalizer.toLegacy(path);

                    switch (store) {
                        case CONFIGURATION: {
                            oldTx.putConfigurationData(legacyPath, legacyData);
                            return;
                        }
                    }

                    throw new IllegalArgumentException("Cannot put data " + path + " to datastore " + store);
                } catch (final DataNormalizationException e) {
                    throw new IllegalArgumentException(String.format("Cannot transform path %s to legacy format", path), e);
                }
            }

            @Override
            public void merge(final LogicalDatastoreType store, final InstanceIdentifier path, final NormalizedNode<?, ?> data) {
                // TODO not supported
                throw new UnsupportedOperationException("Merge not supported for mount point");
            }

            @Override
            public void delete(final LogicalDatastoreType store, final InstanceIdentifier path) {
                try {
                    final InstanceIdentifier legacyPath = dataNormalizer.toLegacy(path);

                    switch (store) {
                        case CONFIGURATION: {
                            oldTx.removeConfigurationData(legacyPath);
                            return;
                        }
                    }
                    throw new IllegalArgumentException("Cannot delete data " + path + " from datastore " + store);
                } catch (final DataNormalizationException e) {
                    throw new IllegalArgumentException(String.format("Cannot transform path %s to legacy format", path), e);
                }
            }

            @Override
            public CheckedFuture<Void, TransactionCommitFailedException> submit() {
                final ListenableFuture<Void> commitAsVoid = Futures.transform(commit(), new Function<RpcResult<TransactionStatus>, Void>() {
                    @Override
                    public Void apply(@Nullable final RpcResult<TransactionStatus> input) {
                        return null;
                    }
                });

                return Futures.makeChecked(commitAsVoid, new Function<Exception, TransactionCommitFailedException>() {
                    @Override
                    public TransactionCommitFailedException apply(@Nullable final Exception input) {
                        return new TransactionCommitFailedException("Commit failed", input);
                    }
                });
            }

            @Override
            public ListenableFuture<RpcResult<TransactionStatus>> commit() {
                return JdkFutureAdapters.listenInPoolThread(oldTx.commit());
            }
        }


        @VisibleForTesting
        static class BackwardsCompatibleReadWriteTransaction implements DOMDataReadWriteTransaction {

            private final DataProviderService dataReader;
            private final DataNormalizer dataNormalizer;
            private final BackwardsCompatibleWriteTransaction delegateWriteTx;

            public BackwardsCompatibleReadWriteTransaction(final DataProviderService dataReader, final DataNormalizer dataNormalizer) {
                this.dataReader = dataReader;
                this.dataNormalizer = dataNormalizer;
                this.delegateWriteTx = new BackwardsCompatibleWriteTransaction(dataReader, dataNormalizer);
            }

            @Override
            public Object getIdentifier() {
                return this;
            }

            @Override
            public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store, final InstanceIdentifier path) {
                return new BackwardsCompatibleReadTransaction(dataReader, dataNormalizer).read(store, path);
            }

            @Override
            public boolean cancel() {
                return delegateWriteTx.cancel();
            }

            @Override
            public void put(final LogicalDatastoreType store, final InstanceIdentifier path, final NormalizedNode<?, ?> data) {
                delegateWriteTx.put(store, path, data);
            }

            @Override
            public void merge(final LogicalDatastoreType store, final InstanceIdentifier path, final NormalizedNode<?, ?> data) {
                delegateWriteTx.merge(store, path, data);
            }

            @Override
            public void delete(final LogicalDatastoreType store, final InstanceIdentifier path) {
                delegateWriteTx.delete(store, path);
            }

            @Override
            public CheckedFuture<Void, TransactionCommitFailedException> submit() {
                return delegateWriteTx.submit();
            }

            @Override
            public ListenableFuture<RpcResult<TransactionStatus>> commit() {
                return delegateWriteTx.commit();
            }
        }
    }

    private class DelgatingNotificationPublishService implements NotificationPublishService {
        private final NotificationRouter notificationRouter;

        public DelgatingNotificationPublishService(final NotificationRouter notificationRouter) {
            this.notificationRouter = notificationRouter;
        }

        private DelgatingNotificationPublishService() {
            this(new NotificationRouterImpl());
        }

        @Override
        public void publish(final CompositeNode notification) {
            notificationRouter.publish(notification);
        }

        @Override
        public ListenerRegistration<NotificationListener> addNotificationListener(final QName notification, final NotificationListener listener) {
            return notificationRouter.addNotificationListener(notification, listener);
        }
    }
}
