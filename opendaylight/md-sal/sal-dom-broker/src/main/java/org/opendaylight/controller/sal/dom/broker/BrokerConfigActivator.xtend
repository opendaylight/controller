/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker

import java.util.concurrent.Executors
import java.util.Hashtable

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker
import org.opendaylight.controller.md.sal.dom.broker.impl.compat.BackwardsCompatibleDataBroker
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMDataBrokerImpl
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore
import org.opendaylight.controller.sal.core.api.data.DataBrokerService
import org.opendaylight.controller.sal.core.api.data.DataProviderService
import org.opendaylight.controller.sal.core.api.data.DataStore
import org.opendaylight.controller.sal.core.api.model.SchemaService
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService
import org.opendaylight.controller.sal.core.api.mount.MountService
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry
import org.opendaylight.controller.sal.core.spi.data.DOMStore
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareDataStoreAdapter
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareRpcBroker
import org.opendaylight.controller.sal.dom.broker.impl.SchemaContextProviders
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener
import org.opendaylight.yangtools.yang.model.api.SchemaServiceListener

import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceRegistration

import com.google.common.collect.ImmutableMap
import com.google.common.util.concurrent.MoreExecutors

class BrokerConfigActivator implements AutoCloseable {

    private static val ROOT = InstanceIdentifier.builder().toInstance();

    @Property
    private var DataProviderService dataService;

    private var ServiceRegistration<DataBrokerService> dataReg;
    private var ServiceRegistration<DataProviderService> dataProviderReg;
    private var ServiceRegistration<MountService> mountReg;
    private var ServiceRegistration<MountProvisionService> mountProviderReg;
    private var SchemaService schemaService;
    private var ServiceRegistration<RpcProvisionRegistry> rpcProvisionRegistryReg;
    private var MountPointManagerImpl mountService;

    SchemaAwareDataStoreAdapter wrappedStore

    public def void start(BrokerImpl broker, DataStore store, DOMDataBroker asyncBroker,BundleContext context) {
        val emptyProperties = new Hashtable<String, String>();
        broker.setBundleContext(context);

        val serviceRef = context.getServiceReference(SchemaService);
        schemaService = context.getService(serviceRef);

        broker.setRouter(new SchemaAwareRpcBroker("/", SchemaContextProviders.fromSchemaService(schemaService)));
        

        if(asyncBroker == null) {
            dataService = new DataBrokerImpl();
            dataProviderReg = context.registerService(DataProviderService, dataService, emptyProperties);
    
            wrappedStore = new SchemaAwareDataStoreAdapter();
            wrappedStore.changeDelegate(store);
            wrappedStore.setValidationEnabled(false);
            context.registerService(SchemaContextListener, wrappedStore, emptyProperties)
            context.registerService(SchemaServiceListener, wrappedStore, emptyProperties)
            
            dataService.registerConfigurationReader(ROOT, wrappedStore);
            dataService.registerCommitHandler(ROOT, wrappedStore);
            dataService.registerOperationalReader(ROOT, wrappedStore);
        } else {
            val compatibleDataBroker = new BackwardsCompatibleDataBroker(asyncBroker);
            context.registerService(SchemaContextListener,compatibleDataBroker,emptyProperties);
            context.registerService(SchemaServiceListener,compatibleDataBroker,emptyProperties);
            dataService = compatibleDataBroker;
        }
        

//        

        mountService = new MountPointManagerImpl();
        dataReg = context.registerService(DataBrokerService, dataService, emptyProperties);
            mountReg = context.registerService(MountService, mountService, emptyProperties);
        mountProviderReg = context.registerService(MountProvisionService, mountService, emptyProperties);

        rpcProvisionRegistryReg = context.registerService(RpcProvisionRegistry, broker.getRouter(), emptyProperties);
    }

    override def close() {
        dataReg?.unregister();
        dataProviderReg?.unregister();
        mountReg?.unregister();
        mountProviderReg?.unregister();
        rpcProvisionRegistryReg?.unregister();
    }

}
