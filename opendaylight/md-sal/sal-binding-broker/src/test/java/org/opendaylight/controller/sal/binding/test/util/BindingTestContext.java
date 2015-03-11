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
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.compat.HeliumRpcProviderRegistry;
import org.opendaylight.controller.md.sal.binding.compat.HydrogenDataBrokerAdapter;
import org.opendaylight.controller.md.sal.binding.compat.HydrogenMountProvisionServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMMountPointServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMRpcProviderServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMRpcServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.impl.ForwardedBindingDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMRpcRouter;
import org.opendaylight.controller.md.sal.dom.broker.impl.SerializedDOMDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.compat.BackwardsCompatibleDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.mount.DOMMountPointServiceImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderService;
import org.opendaylight.controller.sal.binding.impl.NotificationBrokerImpl;
import org.opendaylight.controller.sal.binding.impl.RootBindingAwareBroker;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
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

@Beta
public class BindingTestContext implements AutoCloseable {


    private RuntimeGeneratedMappingServiceImpl mappingServiceImpl;
    private BindingToNormalizedNodeCodec codec;

    private RootBindingAwareBroker baBrokerImpl;

    private NotificationBrokerImpl baNotifyImpl;


    @Deprecated
    private org.opendaylight.controller.sal.core.api.data.DataProviderService biDataLegacyBroker;
    private BrokerImpl biBrokerImpl;

    private final ListeningExecutorService executor;
    private final ClassPool classPool;

    private final boolean startWithSchema;

    private DOMMountPointService biMountImpl;

    private ImmutableMap<LogicalDatastoreType, DOMStore> newDatastores;

    @Deprecated
    private BackwardsCompatibleDataBroker biCompatibleBroker;

    @Deprecated
    private DataProviderService baData;

    private DOMDataBroker newDOMDataBroker;

    private final MockSchemaService mockSchemaService = new MockSchemaService();

    private DataBroker dataBroker;

    private RpcConsumerRegistry baConsumerRpc;

    private BindingDOMRpcProviderServiceAdapter baProviderRpc;
    private DOMRpcRouter domRouter;



    public DOMDataBroker getDomAsyncDataBroker() {
        return newDOMDataBroker;
    }

    public BindingToNormalizedNodeCodec getCodec() {
        return codec;
    }

    protected BindingTestContext(final ListeningExecutorService executor, final ClassPool classPool, final boolean startWithSchema) {
        this.executor = executor;
        this.classPool = classPool;
        this.startWithSchema = startWithSchema;
    }

    public void startDomDataBroker() {
    }

    public void startNewDataBroker() {
        checkState(executor != null, "Executor needs to be set");
        checkState(newDOMDataBroker != null, "DOM Data Broker must be set");
        dataBroker = new ForwardedBindingDataBroker(newDOMDataBroker, codec);
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

    }

    public void startBindingBroker() {
        checkState(executor != null, "Executor needs to be set");
        checkState(baData != null, "Binding Data Broker must be started");
        checkState(baNotifyImpl != null, "Notification Service must be started");

        baConsumerRpc = new BindingDOMRpcServiceAdapter(getDomRpcInvoker(), codec);
        baProviderRpc = new BindingDOMRpcProviderServiceAdapter(getDomRpcRegistry(), codec);

        baBrokerImpl = new RootBindingAwareBroker("test");

        final MountPointService mountService = new BindingDOMMountPointServiceAdapter(biMountImpl, codec);
        baBrokerImpl.setMountService(mountService);
        baBrokerImpl.setLegacyMountManager(new HydrogenMountProvisionServiceAdapter(mountService));
        baBrokerImpl.setRpcBroker(new HeliumRpcProviderRegistry(baConsumerRpc,baProviderRpc));
        baBrokerImpl.setLegacyDataBroker(baData);
        baBrokerImpl.setNotificationBroker(baNotifyImpl);
        baBrokerImpl.start();
    }

    public void startForwarding() {

    }

    private ProviderSession createMockContext() {

        @SuppressWarnings("deprecation")
        final ClassToInstanceMap<BrokerService> domBrokerServices = ImmutableClassToInstanceMap
                .<BrokerService> builder()
                //
                .put(org.opendaylight.controller.sal.core.api.data.DataProviderService.class, biDataLegacyBroker) //
                .put(RpcProvisionRegistry.class, biBrokerImpl.getRouter()) //
                .put(DOMMountPointService.class, biMountImpl)
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
        final HydrogenDataBrokerAdapter forwarded = new HydrogenDataBrokerAdapter(dataBroker);
        baData = forwarded;
    }

    private void startDomMountPoint() {
        biMountImpl = new DOMMountPointServiceImpl();
    }

    private void startDomBroker() {
        checkState(executor != null);

        final SchemaAwareRpcBroker router = new SchemaAwareRpcBroker("/", mockSchemaService);

        domRouter = new DOMRpcRouter();
        mockSchemaService.registerSchemaContextListener(domRouter);

        final ClassToInstanceMap<BrokerService> services = MutableClassToInstanceMap.create();
        services.put(DOMRpcService.class, domRouter);

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

    public DOMRpcProviderService getDomRpcRegistry() {
        return domRouter;
    }

    public DOMRpcService getDomRpcInvoker() {
        return domRouter;
    }

    @Override
    public void close() throws Exception {

    }

    public MountProviderService getBindingMountProviderService() {
        return baBrokerImpl.getLegacyMount();
    }

    public DOMMountPointService getDomMountProviderService() {
        return biMountImpl;
    }

    public DataBroker getDataBroker() {
        return dataBroker;
    }


}
