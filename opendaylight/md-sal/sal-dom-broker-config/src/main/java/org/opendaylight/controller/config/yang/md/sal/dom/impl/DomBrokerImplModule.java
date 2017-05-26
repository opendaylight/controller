/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.dom.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.dom.broker.BrokerImpl;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated Replaced by blueprint wiring
 */
@Deprecated
public final class DomBrokerImplModule extends org.opendaylight.controller.config.yang.md.sal.dom.impl.AbstractDomBrokerImplModule{
    private static final Logger LOG = LoggerFactory.getLogger(DomBrokerImplModule.class);

    private BundleContext bundleContext;

    public DomBrokerImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DomBrokerImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final DomBrokerImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void validate() {
        super.validate();
        final long depth = getNotificationQueueDepth().getValue();
        Preconditions.checkArgument(Long.lowestOneBit(depth) == Long.highestOneBit(depth), "Queue depth %s is not power-of-two", depth);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // The services are provided via blueprint so retrieve then from the OSGi service registry for
        // backwards compatibility.

        final List<AutoCloseable> closeables = new ArrayList<>();
        DOMNotificationService domNotificationService = newTracker(
                DOMNotificationService.class, closeables).waitForService(WaitingServiceTracker.FIVE_MINUTES);

        DOMNotificationPublishService domNotificationPublishService = newTracker(
                DOMNotificationPublishService.class, closeables).waitForService(WaitingServiceTracker.FIVE_MINUTES);

        DOMRpcService domRpcService = newTracker(
                DOMRpcService.class, closeables).waitForService(WaitingServiceTracker.FIVE_MINUTES);

        DOMRpcProviderService domRpcProvider = newTracker(
                DOMRpcProviderService.class, closeables).waitForService(WaitingServiceTracker.FIVE_MINUTES);

        DOMMountPointService mountService = newTracker(DOMMountPointService.class, closeables).
                waitForService(WaitingServiceTracker.FIVE_MINUTES);

        SchemaService globalSchemaService = newTracker(SchemaService.class, closeables).
                waitForService(WaitingServiceTracker.FIVE_MINUTES);

        final DOMDataBroker dataBroker = getAsyncDataBrokerDependency();

        final ClassToInstanceMap<BrokerService> services = MutableClassToInstanceMap.create();

        services.putInstance(DOMNotificationService.class, domNotificationService);
        services.putInstance(DOMNotificationPublishService.class, domNotificationPublishService);

        final SchemaService schemaService = getSchemaServiceImpl(globalSchemaService);
        services.putInstance(SchemaService.class, schemaService);

        services.putInstance(DOMDataBroker.class, dataBroker);

        services.putInstance(DOMRpcService.class, domRpcService);
        services.putInstance(DOMRpcProviderService.class, domRpcProvider);

        services.putInstance(DOMMountPointService.class, mountService);

        BrokerImpl broker = new BrokerImpl(domRpcService, domRpcProvider, services);
        broker.setDeactivator(new AutoCloseable() {
            @Override
            public void close() {
                for(AutoCloseable ac: closeables) {
                    try {
                        ac.close();
                    } catch(Exception e) {
                        LOG.warn("Exception while closing {}", ac, e);
                    }
                }
            }
        });

        return broker;
    }

    private <T> WaitingServiceTracker<T> newTracker(Class<T> serviceInterface, List<AutoCloseable> closeables) {
        WaitingServiceTracker<T> tracker = WaitingServiceTracker.create(serviceInterface, bundleContext);
        closeables.add(tracker);
        return tracker;
    }

    private SchemaService getSchemaServiceImpl(SchemaService globalSchemaService) {
        final SchemaService schemaService;
        if(getRootSchemaService() != null) {
            schemaService = getRootSchemaServiceDependency();
        } else {
            schemaService = globalSchemaService;
        }
        return schemaService;
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
