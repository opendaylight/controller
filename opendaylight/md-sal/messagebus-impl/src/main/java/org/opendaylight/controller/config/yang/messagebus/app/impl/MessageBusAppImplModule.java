/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.messagebus.app.impl;

import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.messagebus.app.impl.EventSourceTopology;
import org.opendaylight.controller.messagebus.eventsources.netconf.NetconfEventSourceManager;
import org.opendaylight.controller.messagebus.spi.EventSource;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistration;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistry;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class MessageBusAppImplModule extends org.opendaylight.controller.config.yang.messagebus.app.impl.AbstractMessageBusAppImplModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageBusAppImplModule.class);

    private BundleContext bundleContext;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public MessageBusAppImplModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public MessageBusAppImplModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver,
            final MessageBusAppImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        final ProviderContext bindingCtx = getBindingBrokerDependency().registerProvider(new Providers.BindingAware());
        final ProviderSession domCtx = getDomBrokerDependency().registerProvider(new Providers.BindingIndependent());
        final DataBroker dataBroker = bindingCtx.getSALService(DataBroker.class);
        final DOMNotificationPublishService domPublish = domCtx.getService(DOMNotificationPublishService.class);
        final DOMMountPointService domMount = domCtx.getService(DOMMountPointService.class);
        final RpcProviderRegistry rpcRegistry = bindingCtx.getSALService(RpcProviderRegistry.class);
        final MountPointService mountPointService = bindingCtx.getSALService(MountPointService.class);
        final EventSourceRegistryWrapper eventSourceRegistryWrapper = new EventSourceRegistryWrapper(new EventSourceTopology(dataBroker, rpcRegistry));
        final NetconfEventSourceManager netconfEventSourceManager
                = NetconfEventSourceManager.create(dataBroker,
                        domPublish,
                        domMount,
                        mountPointService,
                        eventSourceRegistryWrapper,
                        getNamespaceToStream());
        eventSourceRegistryWrapper.addAutoCloseable(netconfEventSourceManager);
        LOGGER.info("Messagebus initialized");
        return eventSourceRegistryWrapper;

    }

    //TODO: separate NetconfEventSource into separate bundle, remove this wrapper, return EventSourceTopology directly as EventSourceRegistry
    private class EventSourceRegistryWrapper implements EventSourceRegistry{

        private final EventSourceRegistry baseEventSourceRegistry;
        private final Set<AutoCloseable> autoCloseables = new HashSet<>();

        public EventSourceRegistryWrapper(EventSourceRegistry baseEventSourceRegistry) {
            this.baseEventSourceRegistry = baseEventSourceRegistry;
        }

        public void addAutoCloseable(AutoCloseable ac){
            Preconditions.checkNotNull(ac);
            autoCloseables.add(ac);
        }

        @Override
        public void close() throws Exception {
            for(AutoCloseable ac : autoCloseables){
                ac.close();
            }
            baseEventSourceRegistry.close();
        }

        @Override
        public <T extends EventSource> EventSourceRegistration<T> registerEventSource(T eventSource) {
            return this.baseEventSourceRegistry.registerEventSource(eventSource);
        }

    }
}
