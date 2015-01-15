/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.dom.impl;

import org.opendaylight.controller.md.sal.dom.broker.compat.hydrogen.BackwardsCompatibleMountPointManager;

import org.opendaylight.controller.md.sal.dom.broker.compat.hydrogen.BackwardsCompatibleDataBroker;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.broker.impl.mount.DOMMountPointServiceImpl;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sal.core.api.mount.MountService;
import org.opendaylight.controller.sal.dom.broker.BrokerImpl;
import org.opendaylight.controller.sal.dom.broker.GlobalBundleScanningSchemaServiceImpl;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareRpcBroker;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaContextProviders;

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


        final SchemaService schemaService = getSchemaServiceImpl();
        services.putInstance(SchemaService.class, schemaService);
        final SchemaAwareRpcBroker router = new SchemaAwareRpcBroker("/", SchemaContextProviders
                .fromSchemaService(schemaService));
        services.putInstance(RpcProvisionRegistry.class, router);

        services.putInstance(DOMDataBroker.class, asyncBroker);
        final DataProviderService legacyData = new BackwardsCompatibleDataBroker(asyncBroker,schemaService);
        services.putInstance(DataProviderService.class,legacyData);
        services.putInstance(DataBrokerService.class, legacyData);

        final DOMMountPointService mountService = new DOMMountPointServiceImpl();
        services.putInstance(DOMMountPointService.class, mountService);

        // TODO remove backwards service, use only new DOMMountPointService
        final MountProvisionService backwardsMountService = new BackwardsCompatibleMountPointManager(mountService);
        services.putInstance(MountService.class, backwardsMountService);
        services.putInstance(MountProvisionService.class, backwardsMountService);

        return new BrokerImpl(router, services);
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
