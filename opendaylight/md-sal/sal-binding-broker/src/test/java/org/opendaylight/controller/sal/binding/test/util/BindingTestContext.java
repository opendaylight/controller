/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.util;

import static com.google.common.base.Preconditions.checkState;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import javassist.ClassPool;

import org.opendaylight.controller.md.sal.binding.impl.ForwardedBackwardsCompatibleDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMDataBrokerImpl;
import org.opendaylight.controller.md.sal.dom.broker.impl.compat.BackwardsCompatibleDataBroker;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderService;
import org.opendaylight.controller.sal.binding.impl.DataBrokerImpl;
import org.opendaylight.controller.sal.binding.impl.NotificationBrokerImpl;
import org.opendaylight.controller.sal.binding.impl.RpcProviderRegistryImpl;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingDomConnectorDeployer;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentConnector;
import org.opendaylight.controller.sal.binding.impl.forward.DomForwardedBindingBrokerImpl;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.controller.sal.core.api.data.DataStore;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.dom.broker.BrokerImpl;
import org.opendaylight.controller.sal.dom.broker.MountPointManagerImpl;
import org.opendaylight.controller.sal.dom.broker.impl.DataStoreStatsWrapper;
import org.opendaylight.controller.sal.dom.broker.impl.HashMapDataStore;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareDataStoreAdapter;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareRpcBroker;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaContextProvider;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.sal.binding.generator.impl.RuntimeGeneratedMappingServiceImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;

public class BindingTestContext implements AutoCloseable, SchemaContextProvider {

    public static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier TREE_ROOT = org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
            .builder().toInstance();

    private static final Logger LOG = LoggerFactory.getLogger(BindingTestContext.class);

    private RuntimeGeneratedMappingServiceImpl mappingServiceImpl;

    private DomForwardedBindingBrokerImpl baBrokerImpl;
    private DataBrokerImpl baDataImpl;
    private NotificationBrokerImpl baNotifyImpl;
    private BindingIndependentConnector baConnectImpl;

    private org.opendaylight.controller.sal.dom.broker.DataBrokerImpl biDataImpl;
    private org.opendaylight.controller.sal.core.api.data.DataProviderService biDataLegacyBroker;
    private BrokerImpl biBrokerImpl;
    private HashMapDataStore rawDataStore;
    private SchemaAwareDataStoreAdapter schemaAwareDataStore;
    private DataStoreStatsWrapper dataStoreStats;
    private DataStore dataStore;

    private final boolean dataStoreStatisticsEnabled = false;

    private final ListeningExecutorService executor;
    private final ClassPool classPool;

    private final boolean startWithSchema;

    private MountPointManagerImpl biMountImpl;

    private SchemaContext schemaContext;

    private ImmutableMap<LogicalDatastoreType, DOMStore> newDatastores;

    private BackwardsCompatibleDataBroker biCompatibleBroker;

    private final List<SchemaContextListener> schemaListeners = new ArrayList<>();

    private DataProviderService baData;

    private DOMDataBroker newDOMDataBroker;

    @Override
    public SchemaContext getSchemaContext() {
        return schemaContext;
    }

    protected BindingTestContext(final ListeningExecutorService executor, final ClassPool classPool, final boolean startWithSchema) {
        this.executor = executor;
        this.classPool = classPool;
        this.startWithSchema = startWithSchema;
    }

    public void startDomDataStore() {
        checkState(dataStore == null, "DataStore already started.");
        checkState(biDataImpl != null, "Dom Data Broker not present");
        rawDataStore = new HashMapDataStore();
        schemaAwareDataStore = new SchemaAwareDataStoreAdapter();
        schemaAwareDataStore.changeDelegate(rawDataStore);
        if (dataStoreStatisticsEnabled) {
            dataStoreStats = new DataStoreStatsWrapper(schemaAwareDataStore);
            dataStore = dataStoreStats;
        } else {
            dataStore = schemaAwareDataStore;
        }

        biDataImpl.registerConfigurationReader(TREE_ROOT, dataStore);
        biDataImpl.registerOperationalReader(TREE_ROOT, dataStore);
        biDataImpl.registerCommitHandler(TREE_ROOT, dataStore);
    }

    public void startDomDataBroker() {
        checkState(executor != null, "Executor needs to be set");
        biDataImpl = new org.opendaylight.controller.sal.dom.broker.DataBrokerImpl();
        biDataImpl.setExecutor(executor);
        biDataLegacyBroker = biDataImpl;
    }

    public void startNewDomDataBroker() {
        checkState(executor != null, "Executor needs to be set");
        InMemoryDOMDataStore operStore = new InMemoryDOMDataStore("OPER", executor);
        InMemoryDOMDataStore configStore = new InMemoryDOMDataStore("CFG", executor);
        newDatastores = ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                .put(LogicalDatastoreType.OPERATIONAL, operStore)
                .put(LogicalDatastoreType.CONFIGURATION, configStore)
                .build();

        newDOMDataBroker = new DOMDataBrokerImpl(newDatastores, executor);

        biCompatibleBroker = new BackwardsCompatibleDataBroker(newDOMDataBroker);

        schemaListeners.add(configStore);
        schemaListeners.add(operStore);
        schemaListeners.add(biCompatibleBroker);
        biDataLegacyBroker = biCompatibleBroker;
    }

    public void startBindingDataBroker() {
        checkState(executor != null, "Executor needs to be set");
        baDataImpl = new DataBrokerImpl();
        baDataImpl.setExecutor(executor);
        baData = baDataImpl;
    }

    public void startBindingBroker() {
        checkState(executor != null, "Executor needs to be set");
        checkState(baData != null, "Binding Data Broker must be started");
        checkState(baNotifyImpl != null, "Notification Service must be started");
        baBrokerImpl = new DomForwardedBindingBrokerImpl("test");

        baBrokerImpl.getMountManager().setDataCommitExecutor(executor);
        baBrokerImpl.getMountManager().setNotificationExecutor(executor);
        baBrokerImpl.setRpcBroker(new RpcProviderRegistryImpl("test"));
        baBrokerImpl.setDataBroker(baData);
        baBrokerImpl.setNotificationBroker(baNotifyImpl);
        baBrokerImpl.start();
    }

    public void startForwarding() {
        checkState(baData != null, "Binding Data Broker needs to be started");
        checkState(biDataLegacyBroker != null, "DOM Data Broker needs to be started.");
        checkState(mappingServiceImpl != null, "DOM Mapping Service needs to be started.");

        baConnectImpl = BindingDomConnectorDeployer.createConnector(getBindingToDomMappingService());
        baConnectImpl.setDomRpcRegistry(getDomRpcRegistry());
        baBrokerImpl.setConnector(baConnectImpl);
        baBrokerImpl.setDomProviderContext(createMockContext());
        baBrokerImpl.startForwarding();
    }

    private ProviderSession createMockContext() {

        final ClassToInstanceMap<BrokerService> domBrokerServices = ImmutableClassToInstanceMap
                .<BrokerService> builder()
                //
                .put(org.opendaylight.controller.sal.core.api.data.DataProviderService.class, biDataLegacyBroker) //
                .put(RpcProvisionRegistry.class, biBrokerImpl.getRouter()) //
                .put(MountProvisionService.class, biMountImpl) //
                .build();

        return new ProviderSession() {

            @Override
            public Future<RpcResult<CompositeNode>> rpc(final QName rpc, final CompositeNode input) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T extends BrokerService> T getService(final Class<T> service) {
                return domBrokerServices.getInstance(service);
            }

            @Override
            public boolean isClosed() {
                return false;
            }

            @Override
            public Set<QName> getSupportedRpcs() {
                return null;
            }

            @Override
            public void close() {
            }

            @Override
            public ListenerRegistration<RpcRegistrationListener> addRpcRegistrationListener(
                    final RpcRegistrationListener listener) {
                return null;
            }

            @Override
            public RpcRegistration addRpcImplementation(final QName rpcType, final RpcImplementation implementation)
                    throws IllegalArgumentException {
                return null;
            }

            @Override
            public RoutedRpcRegistration addRoutedRpcImplementation(final QName rpcType, final RpcImplementation implementation) {
                return null;
            }

            @Override
            public RoutedRpcRegistration addMountedRpcImplementation(final QName rpcType, final RpcImplementation implementation) {
                return null;
            }
        };
    }

    public void startBindingToDomMappingService() {
        checkState(classPool != null, "ClassPool needs to be present");
        mappingServiceImpl = new RuntimeGeneratedMappingServiceImpl();
        mappingServiceImpl.setPool(classPool);
        mappingServiceImpl.init();
    }

    public void updateYangSchema(final String[] files) {
        schemaContext = getContext(files);

        if (schemaAwareDataStore != null) {
            schemaAwareDataStore.onGlobalContextUpdated(schemaContext);
        }
        if (mappingServiceImpl != null) {
            mappingServiceImpl.onGlobalContextUpdated(schemaContext);
        }
        for(SchemaContextListener listener : schemaListeners) {
            listener.onGlobalContextUpdated(schemaContext);
        }
    }

    public static String[] getAllYangFilesOnClasspath() {
        Predicate<String> predicate = new Predicate<String>() {
            @Override
            public boolean apply(final String input) {
                return input.endsWith(".yang");
            }
        };
        Reflections reflection = new Reflections("META-INF.yang", new ResourcesScanner());
        Set<String> result = reflection.getResources(predicate);
        return result.toArray(new String[result.size()]);
    }

    private static SchemaContext getContext(final String[] yangFiles) {
        ClassLoader loader = BindingTestContext.class.getClassLoader();
        List<InputStream> streams = new ArrayList<>();
        for (String string : yangFiles) {
            InputStream stream = loader.getResourceAsStream(string);
            streams.add(stream);
        }
        YangParserImpl parser = new YangParserImpl();
        Set<Module> modules = parser.parseYangModelsFromStreams(streams);
        return parser.resolveSchemaContext(modules);
    }

    public void startLegacy() {
        startBindingDataBroker();
        startBindingNotificationBroker();
        startBindingBroker();
        startDomDataBroker();
        startDomDataStore();
        startDomBroker();
        startDomMountPoint();
        startBindingToDomMappingService();
        startForwarding();
        if (startWithSchema) {
            loadYangSchemaFromClasspath();
        }
    }

    public void start() {
        startNewDomDataBroker();
        startDomBroker();
        startDomMountPoint();
        startBindingToDomMappingService();
        startNewBindingDataBroker();

        startBindingNotificationBroker();
        startBindingBroker();

        startForwarding();
        if (startWithSchema) {
            loadYangSchemaFromClasspath();
        }
    }

    public void startNewBindingDataBroker() {
        ForwardedBackwardsCompatibleDataBroker forwarded = new ForwardedBackwardsCompatibleDataBroker(newDOMDataBroker, mappingServiceImpl, executor);
        schemaListeners.add(forwarded);
        baData = forwarded;
    }

    private void startDomMountPoint() {
        biMountImpl = new MountPointManagerImpl();
        biMountImpl.setDataBroker(getDomDataBroker());
    }

    private void startDomBroker() {
        checkState(executor != null);
        biBrokerImpl = new BrokerImpl();
        biBrokerImpl.setRouter(new SchemaAwareRpcBroker("/", this));

    }

    public void startBindingNotificationBroker() {
        checkState(executor != null);
        baNotifyImpl = new NotificationBrokerImpl(executor);

    }

    public void loadYangSchemaFromClasspath() {
        String[] files = getAllYangFilesOnClasspath();
        updateYangSchema(files);
    }

    public DataProviderService getBindingDataBroker() {
        return baData;
    }

    public org.opendaylight.controller.sal.core.api.data.DataProviderService getDomDataBroker() {
        return biDataLegacyBroker;
    }

    public DataStore getDomDataStore() {
        return dataStore;
    }

    public BindingIndependentMappingService getBindingToDomMappingService() {
        return mappingServiceImpl;
    }

    public void logDataStoreStatistics() {
        if (dataStoreStats == null) {
            return;
        }

        LOG.info("BIDataStore Statistics: Configuration Read Count: {} TotalTime: {} ms AverageTime (ns): {} ms",
                dataStoreStats.getConfigurationReadCount(), dataStoreStats.getConfigurationReadTotalTime(),
                dataStoreStats.getConfigurationReadAverageTime());

        LOG.info("BIDataStore Statistics: Operational Read Count: {} TotalTime: {} ms AverageTime (ns): {} ms",
                dataStoreStats.getOperationalReadCount(), dataStoreStats.getOperationalReadTotalTime(),
                dataStoreStats.getOperationalReadAverageTime());

        LOG.info("BIDataStore Statistics: Request Commit Count: {} TotalTime: {} ms AverageTime (ns): {} ms",
                dataStoreStats.getRequestCommitCount(), dataStoreStats.getRequestCommitTotalTime(),
                dataStoreStats.getRequestCommitAverageTime());
    }

    public RpcProviderRegistry getBindingRpcRegistry() {
        return baBrokerImpl.getRoot();
    }

    public RpcProvisionRegistry getDomRpcRegistry() {
        if (biBrokerImpl == null) {
            return null;
        }
        return biBrokerImpl.getRouter();
    }

    public RpcImplementation getDomRpcInvoker() {
        return biBrokerImpl.getRouter();
    }

    @Override
    public void close() throws Exception {

    }

    public MountProviderService getBindingMountProviderService() {
        return baBrokerImpl.getMountManager();
    }

    public MountProvisionService getDomMountProviderService() {
        return biMountImpl;
    }
}
