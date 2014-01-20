/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import com.google.common.collect.ImmutableClassToInstanceMap;
import org.opendaylight.controller.md.sal.binding.util.AbstractBindingSalProviderInstance;
import org.opendaylight.controller.md.sal.binding.util.BindingContextUtils;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderService;
import org.opendaylight.controller.sal.binding.api.mount.MountService;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Mutable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.google.common.base.Preconditions.checkState;

public class RootBindingAwareBroker implements //
        Mutable, //
        Identifiable<String>, //
        BindingAwareBroker, AutoCloseable,
        RpcProviderRegistry {

    private final static Logger LOG = LoggerFactory.getLogger(RootBindingAwareBroker.class);

    RootSalInstance controllerRoot;

    private final String identifier;

    private RpcProviderRegistry rpcBroker;

    private NotificationProviderService notificationBroker;

    private DataProviderService dataBroker;

    private MountPointManagerImpl mountManager;

    public MountPointManagerImpl getMountManager() {
        return mountManager;
    }

    public void setMountManager(MountPointManagerImpl mountManager) {
        this.mountManager = mountManager;
    }

    private ImmutableClassToInstanceMap<BindingAwareService> supportedConsumerServices;

    private ImmutableClassToInstanceMap<BindingAwareService> supportedProviderServices;

    public RootBindingAwareBroker(String instanceName) {
        this.identifier = instanceName;
        mountManager = new MountPointManagerImpl();
    }

    public String getIdentifier() {
        return identifier;
    }

    public RootSalInstance getRoot() {
        return controllerRoot;
    }

    public DataProviderService getDataBroker() {
        return this.dataBroker;
    }

    public NotificationProviderService getNotificationBroker() {
        return this.notificationBroker;
    }

    public RpcProviderRegistry getRpcProviderRegistry() {
        return this.rpcBroker;
    }

    public RpcProviderRegistry getRpcBroker() {
        return rpcBroker;
    }

    public void setRpcBroker(RpcProviderRegistry rpcBroker) {
        this.rpcBroker = rpcBroker;
    }

    public void setNotificationBroker(NotificationProviderService notificationBroker) {
        this.notificationBroker = notificationBroker;
    }

    public void setDataBroker(DataProviderService dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void start() {
        checkState(controllerRoot == null, "Binding Aware Broker was already started.");
        LOG.info("Starting Binding Aware Broker: {}", identifier);

        controllerRoot = new RootSalInstance(getRpcProviderRegistry(), getNotificationBroker(), getDataBroker());
        

        supportedConsumerServices = ImmutableClassToInstanceMap.<BindingAwareService> builder()
                .put(NotificationService.class, getRoot()) //
                .put(DataBrokerService.class, getRoot()) //
                .put(RpcConsumerRegistry.class, getRoot()) //
                .put(MountService.class, mountManager).build();

        supportedProviderServices = ImmutableClassToInstanceMap.<BindingAwareService> builder()
                .putAll(supportedConsumerServices)
                .put(NotificationProviderService.class, getRoot()) //
                .put(DataProviderService.class, getRoot()) //
                .put(RpcProviderRegistry.class, getRoot()) //
                .put(MountProviderService.class, mountManager).build();
    }

    @Override
    public ConsumerContext registerConsumer(BindingAwareConsumer consumer, BundleContext ctx) {
        checkState(supportedConsumerServices != null, "Broker is not initialized.");
        return BindingContextUtils.createConsumerContextAndInitialize(consumer, supportedConsumerServices);
    }

    @Override
    public ProviderContext registerProvider(BindingAwareProvider provider, BundleContext ctx) {
        checkState(supportedProviderServices != null, "Broker is not initialized.");
        return BindingContextUtils.createProviderContextAndInitialize(provider, supportedProviderServices);
    }

    @Override
    public void close() throws Exception {
        // FIXME: Close all sessions
    }
    
    @Override
    public <T extends RpcService> RoutedRpcRegistration<T> addRoutedRpcImplementation(Class<T> type, T implementation)
            throws IllegalStateException {
        return getRoot().addRoutedRpcImplementation(type, implementation);
    }
    
    @Override
    public <T extends RpcService> RpcRegistration<T> addRpcImplementation(Class<T> type, T implementation)
            throws IllegalStateException {
        return getRoot().addRpcImplementation(type, implementation);
    }
    
    @Override
    public <T extends RpcService> T getRpcService(Class<T> module) {
        return getRoot().getRpcService(module);
    }
    @Override
    public <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(
            L arg0) {
        return getRoot().registerRouteChangeListener(arg0);
    }
    

    public class RootSalInstance extends
            AbstractBindingSalProviderInstance<DataProviderService, NotificationProviderService, RpcProviderRegistry> {

        public RootSalInstance(RpcProviderRegistry rpcRegistry, NotificationProviderService notificationBroker,
                DataProviderService dataBroker) {
            super(rpcRegistry, notificationBroker, dataBroker);
        }
    }
}
