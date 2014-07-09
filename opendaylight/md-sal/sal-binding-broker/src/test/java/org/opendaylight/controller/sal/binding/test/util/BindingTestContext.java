/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.util;

import static com.google.common.base.Preconditions.checkState;

import java.util.Set;
import java.util.concurrent.Future;

import javassist.ClassPool;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.impl.ForwardedBackwardsCompatibleDataBroker;
import org.opendaylight.controller.md.sal.binding.impl.ForwardedBindingDataBroker;
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
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.dom.broker.BrokerImpl;
import org.opendaylight.controller.sal.dom.broker.MountPointManagerImpl;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareRpcBroker;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.impl.RuntimeGeneratedMappingServiceImpl;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.util.concurrent.ListeningExecutorService;

@Beta
public class BindingTestContext implements AutoCloseable {

    public static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier TREE_ROOT = org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
            .builder().toInstance();

    private static final Logger LOG = LoggerFactory.getLogger(BindingTestContext.class);

    private RuntimeGeneratedMappingServiceImpl mappingServiceImpl;

    private DomForwardedBindingBrokerImpl baBrokerImpl;
    private DataBrokerImpl baDataImpl;
    private NotificationBrokerImpl baNotifyImpl;
    private BindingIndependentConnector baConnectImpl;

    private org.opendaylight.controller.sal.dom.broker.DataBrokerImpl biDataImpl;
    @SuppressWarnings("deprecation")
    private org.opendaylight.controller.sal.core.api.data.DataProviderService biDataLegacyBroker;
    private BrokerImpl biBrokerImpl;

    private final ListeningExecutorService executor;
    private final ClassPool classPool;

    private final boolean startWithSchema;

    private MountPointManagerImpl biMountImpl;



    private ImmutableMap<LogicalDatastoreType, DOMStore> newDatastores;

    private BackwardsCompatibleDataBroker biCompatibleBroker;

    @SuppressWarnings("deprecation")
    private DataProviderService baData;

    private DOMDataBroker newDOMDataBroker;

    private final MockSchemaService mockSchemaService = new MockSchemaService();

    private DataBroker dataBroker;



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
        biDataImpl = new org.opendaylight.controller.sal.dom.broker.DataBrokerImpl();
        biDataImpl.setExecutor(executor);
        biDataLegacyBroker = biDataImpl;
    }

    public void startNewDataBroker() {
        checkState(executor != null, "Executor needs to be set");
        checkState(newDOMDataBroker != null, "DOM Data Broker must be set");
        dataBroker = new ForwardedBindingDataBroker(newDOMDataBroker, mappingServiceImpl, mockSchemaService);
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

        biCompatibleBroker = new BackwardsCompatibleDataBroker(newDOMDataBroker,mockSchemaService);

        mockSchemaService.registerSchemaServiceListener(configStore);
        mockSchemaService.registerSchemaServiceListener(operStore);
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
        mockSchemaService.registerSchemaServiceListener(mappingServiceImpl);
    }

    private void updateYangSchema(final ImmutableSet<YangModuleInfo> moduleInfos) {
        mockSchemaService.changeSchema(getContext(moduleInfos));
    }

    private SchemaContext getContext(final ImmutableSet<YangModuleInfo> moduleInfos) {
        ModuleInfoBackedContext ctx = ModuleInfoBackedContext.create();
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
        ForwardedBackwardsCompatibleDataBroker forwarded = new ForwardedBackwardsCompatibleDataBroker(newDOMDataBroker, mappingServiceImpl,mockSchemaService, executor);
        baData = forwarded;
    }

    private void startDomMountPoint() {
        biMountImpl = new MountPointManagerImpl();
        biMountImpl.setDataBroker(getDomDataBroker());
    }

    private void startDomBroker() {
        checkState(executor != null);

        SchemaAwareRpcBroker router = new SchemaAwareRpcBroker("/", mockSchemaService);
        ClassToInstanceMap<BrokerService> services = MutableClassToInstanceMap.create();
        biBrokerImpl = new BrokerImpl(router,services);

    }

    public void startBindingNotificationBroker() {
        checkState(executor != null);
        baNotifyImpl = new NotificationBrokerImpl(executor);

    }

    public void loadYangSchemaFromClasspath() {
        ImmutableSet<YangModuleInfo> moduleInfos = BindingReflections.loadModuleInfos();
        updateYangSchema(moduleInfos);
    }

    @SuppressWarnings("deprecation")
    public DataProviderService getBindingDataBroker() {
        return baData;
    }

    @SuppressWarnings("deprecation")
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
