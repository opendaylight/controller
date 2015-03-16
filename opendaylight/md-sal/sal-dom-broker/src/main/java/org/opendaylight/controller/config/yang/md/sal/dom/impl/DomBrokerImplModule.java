/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.dom.impl;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMRpcRouter;
import org.opendaylight.controller.md.sal.dom.broker.impl.mount.DOMMountPointServiceImpl;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.dom.broker.BrokerImpl;
import org.opendaylight.controller.sal.dom.broker.GlobalBundleScanningSchemaServiceImpl;

/**
*
*/
public final class DomBrokerImplModule extends org.opendaylight.controller.config.yang.md.sal.dom.impl.AbstractDomBrokerImplModule
{

    public DomBrokerImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DomBrokerImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final DomBrokerImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void validate(){
        super.validate();
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final DOMDataBroker asyncBroker= getAsyncDataBrokerDependency();

        final ClassToInstanceMap<BrokerService> services = MutableClassToInstanceMap.create();

        // TODO: retrieve from config subsystem
        final int queueDepth = 1024;

        final DOMNotificationRouter domNotificationRouter = DOMNotificationRouter.create(queueDepth);
        services.putInstance(DOMNotificationService.class, domNotificationRouter);
        services.putInstance(DOMNotificationPublishService.class, domNotificationRouter);

        final SchemaService schemaService = getSchemaServiceImpl();
        services.putInstance(SchemaService.class, schemaService);

        services.putInstance(DOMDataBroker.class, asyncBroker);

        final DOMRpcRouter rpcRouter = new DOMRpcRouter();
        schemaService.registerSchemaContextListener(rpcRouter);
        services.putInstance(DOMRpcService.class, rpcRouter);
        services.putInstance(DOMRpcProviderService.class, rpcRouter);

        final DOMMountPointService mountService = new DOMMountPointServiceImpl();
        services.putInstance(DOMMountPointService.class, mountService);

        return new BrokerImpl(rpcRouter, services);
    }

    private SchemaService getSchemaServiceImpl() {
        final SchemaService schemaService;
        if(getRootSchemaService() != null) {
            schemaService = getRootSchemaServiceDependency();
        } else {
            schemaService = GlobalBundleScanningSchemaServiceImpl.getInstance();
        }
        return schemaService;
    }
}
