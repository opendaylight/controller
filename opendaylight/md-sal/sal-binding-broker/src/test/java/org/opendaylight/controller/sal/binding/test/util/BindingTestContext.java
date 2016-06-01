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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import javassist.ClassPool;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.compat.HeliumNotificationProviderServiceAdapter;
import org.opendaylight.controller.md.sal.binding.compat.HeliumRpcProviderRegistry;
import org.opendaylight.controller.md.sal.binding.compat.HydrogenDataBrokerAdapter;
import org.opendaylight.controller.md.sal.binding.compat.HydrogenMountProvisionServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMDataBrokerAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMMountPointServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationPublishServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMRpcProviderServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMRpcServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMRpcRouter;
import org.opendaylight.controller.md.sal.dom.broker.impl.SerializedDOMDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.mount.DOMMountPointServiceImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderService;
import org.opendaylight.controller.sal.binding.impl.RootBindingAwareBroker;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.dom.broker.BrokerImpl;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

@Beta
public class BindingTestContext implements AutoCloseable {


    private BindingToNormalizedNodeCodec codec;

    private RootBindingAwareBroker baBrokerImpl;

    private HeliumNotificationProviderServiceAdapter baNotifyImpl;


    private BrokerImpl biBrokerImpl;

    private final ListeningExecutorService executor;
    private final ClassPool classPool;

    private final boolean startWithSchema;

    private DOMMountPointService biMountImpl;

    private ImmutableMap<LogicalDatastoreType, DOMStore> newDatastores;

    @Deprecated
    private DataProviderService baData;

    private DOMDataBroker newDOMDataBroker;

    private final MockSchemaService mockSchemaService = new MockSchemaService();

    private DataBroker dataBroker;

    private RpcConsumerRegistry baConsumerRpc;

    private BindingDOMRpcProviderServiceAdapter baProviderRpc;
    private DOMRpcRouter domRouter;

    private NotificationPublishService publishService;

    private NotificationService listenService;

    private DOMNotificationPublishService domPublishService;

    private DOMNotificationService domListenService;



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
        dataBroker = new BindingDOMDataBrokerAdapter(newDOMDataBroker, codec);
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

        mockSchemaService.registerSchemaContextListener(configStore);
        mockSchemaService.registerSchemaContextListener(operStore);
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
        baBrokerImpl.setRpcBroker(new HeliumRpcProviderRegistry(baConsumerRpc, baProviderRpc));
        baBrokerImpl.setLegacyDataBroker(baData);
        baBrokerImpl.setNotificationBroker(baNotifyImpl);
        baBrokerImpl.start();
    }

    public void startForwarding() {

    }

    public void startBindingToDomMappingService() {
        checkState(classPool != null, "ClassPool needs to be present");

        final DataObjectSerializerGenerator generator = StreamWriterGenerator.create(JavassistUtils.forClassPool(classPool));
        final BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(generator);
        final GeneratedClassLoadingStrategy loading = GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy();
        codec = new BindingToNormalizedNodeCodec(loading,  codecRegistry);
        mockSchemaService.registerSchemaContextListener(codec);
    }

    private void updateYangSchema(final ImmutableSet<YangModuleInfo> moduleInfos) {
        mockSchemaService.changeSchema(getContext(moduleInfos));
    }

    private static SchemaContext getContext(final ImmutableSet<YangModuleInfo> moduleInfos) {
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

        domRouter = new DOMRpcRouter();
        mockSchemaService.registerSchemaContextListener(domRouter);

        final ClassToInstanceMap<BrokerService> services = MutableClassToInstanceMap.create();
        services.put(DOMRpcService.class, domRouter);

        biBrokerImpl = new BrokerImpl(domRouter,services);

    }

    public void startBindingNotificationBroker() {
        checkState(executor != null);
        final DOMNotificationRouter router = DOMNotificationRouter.create(16);
        domPublishService = router;
        domListenService = router;
        publishService = new BindingDOMNotificationPublishServiceAdapter(codec, domPublishService);
        listenService = new BindingDOMNotificationServiceAdapter(codec, domListenService);
        baNotifyImpl = new HeliumNotificationProviderServiceAdapter(publishService,listenService);

    }

    public void loadYangSchemaFromClasspath() {
        final ImmutableSet<YangModuleInfo> moduleInfos = BindingReflections.loadModuleInfos();
        updateYangSchema(moduleInfos);
    }

    @Deprecated
    public DataProviderService getBindingDataBroker() {
        return baData;
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
