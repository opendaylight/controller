/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.util;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.Beta;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Set;
import java.util.concurrent.Future;
import javassist.ClassPool;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.impl.ForwardedBackwardsCompatibleDataBroker;
import org.opendaylight.controller.md.sal.binding.impl.ForwardedBindingDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.compat.hydrogen.BackwardsCompatibleDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.compat.hydrogen.BackwardsCompatibleMountPointManager;
import org.opendaylight.controller.md.sal.dom.broker.impl.SerializedDOMDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.mount.DOMMountPointServiceImpl;
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
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.dom.broker.BrokerImpl;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareRpcBroker;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.impl.RuntimeGeneratedMappingServiceImpl;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
public class BindingTestContext implements AutoCloseable {

    public static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier TREE_ROOT = org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier
            .builder().toInstance();

    private static final Logger LOG = LoggerFactory.getLogger(BindingTestContext.class);

    private RuntimeGeneratedMappingServiceImpl mappingServiceImpl;
    private BindingToNormalizedNodeCodec codec;

    private DomForwardedBindingBrokerImpl baBrokerImpl;
    private DataBrokerImpl baDataImpl;
    private NotificationBrokerImpl baNotifyImpl;
    private BindingIndependentConnector baConnectImpl;

    private org.opendaylight.controller.md.sal.dom.broker.compat.hydrogen.DataBrokerImpl biDataImpl;
    @SuppressWarnings("deprecation")
    private org.opendaylight.controller.sal.core.api.data.DataProviderService biDataLegacyBroker;
    private BrokerImpl biBrokerImpl;

    private final ListeningExecutorService executor;
    private final ClassPool classPool;

    private final boolean startWithSchema;

    private BackwardsCompatibleMountPointManager biMountImpl;



    private ImmutableMap<LogicalDatastoreType, DOMStore> newDatastores;

    private BackwardsCompatibleDataBroker biCompatibleBroker;

    @SuppressWarnings("deprecation")
    private DataProviderService baData;

    private DOMDataBroker newDOMDataBroker;

    private final MockSchemaService mockSchemaService = new MockSchemaService();

    private DataBroker dataBroker;

    private DOMMountPointServiceImpl domMountImpl;



    public DOMDataBroker getDomAsyncDataBroker() {
        return newDOMDataBroker;
    }

    protected BindingTestContext(final ListeningExecutorService executor, final ClassPool classPool, final boolean startWithSchema) {
        this.executor = executor;
        this.classPool = classPool;
        this.startWithSchema = startWithSchema;
    }

    public void startDomDataBroker() {
        checkState(executor != null, "Executor needs to be set");
        biDataImpl = new org.opendaylight.controller.md.sal.dom.broker.compat.hydrogen.DataBrokerImpl();
        biDataImpl.setExecutor(executor);
        biDataLegacyBroker = biDataImpl;
    }

    public void startNewDataBroker() {
        checkState(executor != null, "Executor needs to be set");
        checkState(newDOMDataBroker != null, "DOM Data Broker must be set");
        dataBroker = new ForwardedBindingDataBroker(newDOMDataBroker, codec, mockSchemaService);
    }

    public void startNewDomDataBroker() {
        checkState(executor != null, "Executor needs to be set");
        final InMemoryDOMDataStore operStore = new InMemoryDOMDataStore("OPER", MoreExecutors.sameThreadExecutor());
        final InMemoryDOMDataStore configStore = new InMemoryDOMDataStore("CFG", MoreExecutors.sameThreadExecutor());
        newDatastores = ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                .put(LogicalDatastoreType.OPERATIONAL, operStore)
                .put(LogicalDatastoreType.CONFIGURATION, configStore)
                .build();

        newDOMDataBroker = new SerializedDOMDataBroker(newDatastores, executor);

        biCompatibleBroker = new BackwardsCompatibleDataBroker(newDOMDataBroker,mockSchemaService);

        mockSchemaService.registerSchemaContextListener(configStore);
        mockSchemaService.registerSchemaContextListener(operStore);
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
        baBrokerImpl.setLegacyDataBroker(baData);
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

        @SuppressWarnings("deprecation")
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
        mappingServiceImpl = new RuntimeGeneratedMappingServiceImpl(classPool);
        mockSchemaService.registerSchemaContextListener(mappingServiceImpl);

        final DataObjectSerializerGenerator generator = StreamWriterGenerator.create(JavassistUtils.forClassPool(classPool));
        final BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(generator);
        final GeneratedClassLoadingStrategy loading = GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy();
        codec = new BindingToNormalizedNodeCodec(loading, mappingServiceImpl, codecRegistry);
        mockSchemaService.registerSchemaContextListener(codec);
    }

    private void updateYangSchema(final ImmutableSet<YangModuleInfo> moduleInfos) {
        mockSchemaService.changeSchema(getContext(moduleInfos));
    }

    private SchemaContext getContext(final ImmutableSet<YangModuleInfo> moduleInfos) {
        final ModuleInfoBackedContext ctx = ModuleInfoBackedContext.create();
        ctx.addModuleInfos(moduleInfos);
        return ctx.tryToCreateSchemaContext().get();
    }

    public void start() {
        startNewDomDataBroker();

        startDomBroker();
        startDomMountPoint();
        startBindingToDomMappingService();
        startNewDataBroker();
        startNewBindingDataBroker();
        startBindingNotificationBroker();
        startBindingBroker();

        startForwarding();
        if (startWithSchema) {
            loadYangSchemaFromClasspath();
        }
    }

    public void startNewBindingDataBroker() {
        final ForwardedBackwardsCompatibleDataBroker forwarded = new ForwardedBackwardsCompatibleDataBroker(newDOMDataBroker, codec,mockSchemaService, executor);
        baData = forwarded;
    }

    private void startDomMountPoint() {
        domMountImpl = new DOMMountPointServiceImpl();
        biMountImpl = new BackwardsCompatibleMountPointManager(domMountImpl);
    }

    private void startDomBroker() {
        checkState(executor != null);

        final SchemaAwareRpcBroker router = new SchemaAwareRpcBroker("/", mockSchemaService);
        final ClassToInstanceMap<BrokerService> services = MutableClassToInstanceMap.create();
        biBrokerImpl = new BrokerImpl(router,services);

    }

    public void startBindingNotificationBroker() {
        checkState(executor != null);
        baNotifyImpl = new NotificationBrokerImpl(executor);

    }

    public void loadYangSchemaFromClasspath() {
        final ImmutableSet<YangModuleInfo> moduleInfos = BindingReflections.loadModuleInfos();
        updateYangSchema(moduleInfos);
    }

    @Deprecated
    public DataProviderService getBindingDataBroker() {
        return baData;
    }

    @Deprecated
    public org.opendaylight.controller.sal.core.api.data.DataProviderService getDomDataBroker() {
        return biDataLegacyBroker;
    }

    public BindingIndependentMappingService getBindingToDomMappingService() {
        return mappingServiceImpl;
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

    public DataBroker getDataBroker() {
        return dataBroker;
    }


}
