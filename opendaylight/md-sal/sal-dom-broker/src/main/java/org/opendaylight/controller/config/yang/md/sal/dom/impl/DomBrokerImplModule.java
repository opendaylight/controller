/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.dom.impl;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.compat.BackwardsCompatibleDataBroker;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.data.DataStore;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sal.core.api.mount.MountService;
import org.opendaylight.controller.sal.dom.broker.BrokerImpl;
import org.opendaylight.controller.sal.dom.broker.DataBrokerImpl;
import org.opendaylight.controller.sal.dom.broker.GlobalBundleScanningSchemaServiceImpl;
import org.opendaylight.controller.sal.dom.broker.MountPointManagerImpl;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareDataStoreAdapter;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareRpcBroker;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaContextProviders;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

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
        final DataStore legacyStore = getDataStoreDependency();
        final DOMDataBroker asyncBroker= getAsyncDataBrokerDependency();

        ClassToInstanceMap<BrokerService> services = MutableClassToInstanceMap.create();


        SchemaService schemaService = getSchemaServiceImpl();
        services.putInstance(SchemaService.class, schemaService);
        SchemaAwareRpcBroker router = new SchemaAwareRpcBroker("/", SchemaContextProviders
                .fromSchemaService(schemaService));
        services.putInstance(RpcProvisionRegistry.class, router);

        final DataProviderService legacyData;
        if(asyncBroker != null) {
            services.putInstance(DOMDataBroker.class, asyncBroker);
            legacyData = new BackwardsCompatibleDataBroker(asyncBroker,schemaService);
        } else {
            legacyData = createLegacyDataService(legacyStore,schemaService);
        }
        services.putInstance(DataProviderService.class,legacyData);
        services.putInstance(DataBrokerService.class, legacyData);


        MountPointManagerImpl mountService = new MountPointManagerImpl();
        services.putInstance(MountService.class, mountService);
        services.putInstance(MountProvisionService.class, mountService);

        return new BrokerImpl(router, services);
    }

    private DataProviderService createLegacyDataService(final DataStore legacyStore, final SchemaService schemaService) {
        InstanceIdentifier rootPath = InstanceIdentifier.builder().toInstance();
        DataBrokerImpl dataService = new DataBrokerImpl();
        SchemaAwareDataStoreAdapter wrappedStore = new SchemaAwareDataStoreAdapter();
        wrappedStore.changeDelegate(legacyStore);
        wrappedStore.setValidationEnabled(false);

        schemaService.registerSchemaServiceListener(wrappedStore);

        dataService.registerConfigurationReader(rootPath, wrappedStore);
        dataService.registerCommitHandler(rootPath, wrappedStore);
        dataService.registerOperationalReader(rootPath, wrappedStore);
        return dataService;
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
