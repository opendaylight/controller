/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableClassToInstanceMap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
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
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Mutable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RootBindingAwareBroker implements Mutable, Identifiable<String>, BindingAwareBroker, AutoCloseable,
        RpcProviderRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(RootBindingAwareBroker.class);

    RootSalInstance controllerRoot;

    private final String identifier;

    private RpcProviderRegistry rpcBroker;

    private NotificationProviderService notificationBroker;

    private NotificationPublishService notificationPublishService;

    private DataBroker dataBroker;

    private ImmutableClassToInstanceMap<BindingAwareService> supportedConsumerServices;

    private ImmutableClassToInstanceMap<BindingAwareService> supportedProviderServices;

    private MountPointService mountService;

    public RootBindingAwareBroker(final String instanceName) {
        this.identifier = instanceName;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    public RootSalInstance getRoot() {
        return controllerRoot;
    }

    public NotificationProviderService getNotificationBroker() {
        return this.notificationBroker;
    }

    public NotificationPublishService getNotificationPublishService() {
        return this.notificationPublishService;
    }

    public RpcProviderRegistry getRpcProviderRegistry() {
        return this.rpcBroker;
    }

    public RpcProviderRegistry getRpcBroker() {
        return rpcBroker;
    }

    public MountPointService getMountService() {
        return mountService;
    }

    public void setDataBroker(final DataBroker asyncDataBroker) {
        dataBroker = asyncDataBroker;
    }

    public void setMountService(final MountPointService mount) {
        this.mountService = mount;
    }

    public void setRpcBroker(final RpcProviderRegistry rpcBroker) {
        this.rpcBroker = rpcBroker;
    }

    public void setNotificationBroker(final NotificationProviderService notificationBroker) {
        this.notificationBroker = notificationBroker;
    }

    public void setNotificationPublishService(final NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
    }

    public void start() {
        checkState(controllerRoot == null, "Binding Aware Broker was already started.");
        LOG.info("Starting Binding Aware Broker: {}", identifier);

        controllerRoot = new RootSalInstance(getRpcProviderRegistry(), getNotificationBroker());

        final ImmutableClassToInstanceMap.Builder<BindingAwareService> consBuilder = ImmutableClassToInstanceMap
                .builder();

        consBuilder.put(NotificationService.class, getRoot());
        consBuilder.put(RpcConsumerRegistry.class, getRoot());
        if (dataBroker != null) {
            consBuilder.put(DataBroker.class, dataBroker);
        }
        consBuilder.put(MountPointService.class, mountService);

        supportedConsumerServices = consBuilder.build();
        final ImmutableClassToInstanceMap.Builder<BindingAwareService> provBuilder = ImmutableClassToInstanceMap
                .builder();
        provBuilder.putAll(supportedConsumerServices).put(NotificationProviderService.class, getRoot())
                .put(RpcProviderRegistry.class, getRoot());
        if (notificationPublishService != null) {
            provBuilder.put(NotificationPublishService.class, notificationPublishService);
        }

        supportedProviderServices = provBuilder.build();
    }

    @Override
    public ConsumerContext registerConsumer(final BindingAwareConsumer consumer, final BundleContext ctx) {
        return registerConsumer(consumer);
    }

    @Override
    public ConsumerContext registerConsumer(final BindingAwareConsumer consumer) {
        checkState(supportedConsumerServices != null, "Broker is not initialized.");
        return BindingContextUtils.createConsumerContextAndInitialize(consumer, supportedConsumerServices);
    }

    @Override
    public ProviderContext registerProvider(final BindingAwareProvider provider, final BundleContext ctx) {
        return registerProvider(provider);
    }

    @Override
    public ProviderContext registerProvider(final BindingAwareProvider provider) {
        checkState(supportedProviderServices != null, "Broker is not initialized.");
        return BindingContextUtils.createProviderContextAndInitialize(provider, supportedProviderServices);
    }

    @Override
    public void close() throws Exception {
        // FIXME: Close all sessions
    }

    @Override
    public <T extends RpcService> RoutedRpcRegistration<T> addRoutedRpcImplementation(final Class<T> type,
            final T implementation) throws IllegalStateException {
        return getRoot().addRoutedRpcImplementation(type, implementation);
    }

    @Override
    public <T extends RpcService> RpcRegistration<T> addRpcImplementation(final Class<T> type, final T implementation)
            throws IllegalStateException {
        return getRoot().addRpcImplementation(type, implementation);
    }

    @Override
    public <T extends RpcService> T getRpcService(final Class<T> module) {
        return getRoot().getRpcService(module);
    }

    @Override
    public <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> ListenerRegistration<L>
            registerRouteChangeListener(final L listener) {
        return getRoot().registerRouteChangeListener(listener);
    }

    public class RootSalInstance extends
            AbstractBindingSalProviderInstance<NotificationProviderService, RpcProviderRegistry> {

        public RootSalInstance(final RpcProviderRegistry rpcRegistry,
                final NotificationProviderService notificationBroker) {
            super(rpcRegistry, notificationBroker);
        }
    }
}
