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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Map;
import javassist.ClassPool;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.compat.HeliumNotificationProviderServiceAdapter;
import org.opendaylight.controller.md.sal.binding.compat.HeliumRpcProviderRegistry;
import org.opendaylight.controller.md.sal.binding.impl.BindingDataBrokerAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingMountPointServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingNotificationPublishServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingNotificationServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingRpcProviderServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingRpcServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMRpcRouter;
import org.opendaylight.controller.md.sal.dom.broker.impl.mount.DOMMountPointServiceImpl;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.impl.RootBindingAwareBroker;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMDataBrokerAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMMountPointServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMNotificationPublishServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMNotificationServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMRpcProviderServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMRpcServiceAdapter;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.broker.SerializedDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

@Beta
public class BindingTestContext implements AutoCloseable {
    private BindingToNormalizedNodeCodec codec;

    private RootBindingAwareBroker baBrokerImpl;

    private HeliumNotificationProviderServiceAdapter baNotifyImpl;

    private final ListeningExecutorService executor;
    private final ClassPool classPool;

    private final boolean startWithSchema;

    private DOMMountPointService biMountImpl;

    private final MockSchemaService mockSchemaService = new MockSchemaService();

    private DataBroker dataBroker;

    private RpcConsumerRegistry baConsumerRpc;

    private BindingRpcProviderServiceAdapter baProviderRpc;
    private DOMRpcRouter domRouter;

    private NotificationPublishService publishService;

    private NotificationService listenService;

    private DOMNotificationPublishService domPublishService;

    private DOMNotificationService domListenService;

    public BindingToNormalizedNodeCodec getCodec() {
        return this.codec;
    }

    protected BindingTestContext(final ListeningExecutorService executor, final ClassPool classPool,
            final boolean startWithSchema) {
        this.executor = executor;
        this.classPool = classPool;
        this.startWithSchema = startWithSchema;
    }

    public void startDomDataBroker() {
    }

    public void startNewDataBroker() {
        checkState(this.executor != null, "Executor needs to be set");
        final InMemoryDOMDataStore operStore = new InMemoryDOMDataStore("OPER",
            MoreExecutors.newDirectExecutorService());
        final InMemoryDOMDataStore configStore = new InMemoryDOMDataStore("CFG",
            MoreExecutors.newDirectExecutorService());
        Map<LogicalDatastoreType, DOMStore> newDatastores = ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                .put(LogicalDatastoreType.OPERATIONAL, operStore)
                .put(LogicalDatastoreType.CONFIGURATION, configStore)
                .build();

        DOMDataBroker newDOMDataBroker = new SerializedDOMDataBroker(newDatastores, this.executor);

        this.mockSchemaService.registerSchemaContextListener(configStore);
        this.mockSchemaService.registerSchemaContextListener(operStore);

        this.dataBroker = new BindingDataBrokerAdapter(new BindingDOMDataBrokerAdapter(newDOMDataBroker, codec));
    }

    public void startBindingBroker() {
        checkState(this.executor != null, "Executor needs to be set");
        checkState(this.baNotifyImpl != null, "Notification Service must be started");

        final org.opendaylight.mdsal.dom.broker.DOMRpcRouter mdsalDomRouter =
                org.opendaylight.mdsal.dom.broker.DOMRpcRouter.newInstance(mockSchemaService);
        this.domRouter = new DOMRpcRouter(mdsalDomRouter, mdsalDomRouter);

        this.baConsumerRpc = new BindingRpcServiceAdapter(new BindingDOMRpcServiceAdapter(
                mdsalDomRouter, this.codec));
        this.baProviderRpc = new BindingRpcProviderServiceAdapter(
                new BindingDOMRpcProviderServiceAdapter(mdsalDomRouter, this.codec));

        this.baBrokerImpl = new RootBindingAwareBroker("test");

        final org.opendaylight.mdsal.dom.broker.DOMMountPointServiceImpl mdsalMountService =
                new org.opendaylight.mdsal.dom.broker.DOMMountPointServiceImpl();
        this.biMountImpl = new DOMMountPointServiceImpl(mdsalMountService);

        final MountPointService mountService = new BindingMountPointServiceAdapter(
                new BindingDOMMountPointServiceAdapter(mdsalMountService, this.codec));
        this.baBrokerImpl.setMountService(mountService);
        this.baBrokerImpl.setRpcBroker(new HeliumRpcProviderRegistry(this.baConsumerRpc, this.baProviderRpc));
        this.baBrokerImpl.setNotificationBroker(this.baNotifyImpl);
        this.baBrokerImpl.start();
    }

    public void startForwarding() {

    }

    public void startBindingToDomMappingService() {
        checkState(this.classPool != null, "ClassPool needs to be present");

        final DataObjectSerializerGenerator generator = StreamWriterGenerator.create(
                JavassistUtils.forClassPool(this.classPool));
        final BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(generator);
        final GeneratedClassLoadingStrategy loading = GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy();
        this.codec = new BindingToNormalizedNodeCodec(loading,  codecRegistry);
        this.mockSchemaService.registerSchemaContextListener(this.codec);
    }

    private void updateYangSchema(final ImmutableSet<YangModuleInfo> moduleInfos) {
        this.mockSchemaService.changeSchema(getContext(moduleInfos));
    }

    private SchemaContext getContext(final ImmutableSet<YangModuleInfo> moduleInfos) {
        final ModuleInfoBackedContext ctx = ModuleInfoBackedContext.create();
        ctx.addModuleInfos(moduleInfos);
        return ctx.tryToCreateSchemaContext().get();
    }

    public void start() {
        startBindingToDomMappingService();
        startNewDataBroker();
        startBindingNotificationBroker();
        startBindingBroker();

        startForwarding();
        if (this.startWithSchema) {
            loadYangSchemaFromClasspath();
        }
    }

    public void startBindingNotificationBroker() {
        checkState(this.executor != null);
        org.opendaylight.mdsal.dom.broker.DOMNotificationRouter mdsalDomRouter =
                org.opendaylight.mdsal.dom.broker.DOMNotificationRouter.create(16);
        final DOMNotificationRouter router =
                DOMNotificationRouter.create(mdsalDomRouter, mdsalDomRouter, mdsalDomRouter);
        this.domPublishService = router;
        this.domListenService = router;
        this.publishService = new BindingNotificationPublishServiceAdapter(
                new BindingDOMNotificationPublishServiceAdapter(mdsalDomRouter, this.codec));
        this.listenService = new BindingNotificationServiceAdapter(
                new BindingDOMNotificationServiceAdapter(mdsalDomRouter, this.codec));
        this.baNotifyImpl = new HeliumNotificationProviderServiceAdapter(this.publishService, this.listenService);
    }

    public void loadYangSchemaFromClasspath() {
        final ImmutableSet<YangModuleInfo> moduleInfos = BindingReflections.loadModuleInfos();
        updateYangSchema(moduleInfos);
    }

    public RpcProviderRegistry getBindingRpcRegistry() {
        return this.baBrokerImpl.getRoot();
    }

    public DOMRpcProviderService getDomRpcRegistry() {
        return this.domRouter;
    }

    public DOMRpcService getDomRpcInvoker() {
        return this.domRouter;
    }

    @Override
    public void close() throws Exception {

    }

    public MountPointService getBindingMountPointService() {
        return this.baBrokerImpl.getMountService();
    }

    public DOMMountPointService getDomMountProviderService() {
        return this.biMountImpl;
    }

    public DataBroker getDataBroker() {
        return this.dataBroker;
    }
}
