/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker

import java.util.Hashtable
import org.opendaylight.controller.sal.core.api.data.DataBrokerService
import org.opendaylight.controller.sal.core.api.data.DataProviderService
import org.opendaylight.controller.sal.core.api.data.DataStore
import org.opendaylight.controller.sal.core.api.model.SchemaService
import org.opendaylight.yangtools.yang.model.api.SchemaServiceListener
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService
import org.opendaylight.controller.sal.core.api.mount.MountService
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareDataStoreAdapter
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareRpcBroker
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceRegistration
import org.opendaylight.controller.sal.dom.broker.impl.SchemaContextProviders
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry
import org.opendaylight.controller.md.sal.dom.broker.impl.compat.BackwardsCompatibleDataBroker
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMDataBrokerImpl
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.collect.ImmutableMap
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.controller.sal.core.spi.data.DOMStore
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore
import java.util.concurrent.Executors
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker

class BrokerConfigActivator implements AutoCloseable {

    private static val ROOT = InstanceIdentifier.builder().toInstance();

    @Property
    private var BackwardsCompatibleDataBroker dataService;

    private var ServiceRegistration<DataBrokerService> dataReg;
    private var ServiceRegistration<DataProviderService> dataProviderReg;
    private var ServiceRegistration<MountService> mountReg;
    private var ServiceRegistration<MountProvisionService> mountProviderReg;
    private var SchemaService schemaService;
    private var ServiceRegistration<RpcProvisionRegistry> rpcProvisionRegistryReg;
    private var MountPointManagerImpl mountService;

    SchemaAwareDataStoreAdapter wrappedStore

    public def void start(BrokerImpl broker, DataStore store, BundleContext context) {
        val emptyProperties = new Hashtable<String, String>();
        broker.setBundleContext(context);

        val serviceRef = context.getServiceReference(SchemaService);
        schemaService = context.getService(serviceRef);

        broker.setRouter(new SchemaAwareRpcBroker("/", SchemaContextProviders.fromSchemaService(schemaService)));
        val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor);
        val operStore = new InMemoryDOMDataStore("DOM-OPER", executor);
        val configStore = new InMemoryDOMDataStore("DOM-CFG", executor);
        val datastores = ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                .put(LogicalDatastoreType.OPERATIONAL, operStore)
                .put(LogicalDatastoreType.CONFIGURATION, configStore)
                .build();

        val newDataBroker = new DOMDataBrokerImpl(
            datastores,MoreExecutors.sameThreadExecutor
        );

        dataService = new BackwardsCompatibleDataBroker(newDataBroker);

        //dataService.setExecutor(broker.getExecutor());
        dataReg = context.registerService(DataBrokerService, dataService, emptyProperties);
        dataProviderReg = context.registerService(DataProviderService, dataService, emptyProperties);

        wrappedStore = new SchemaAwareDataStoreAdapter();
        wrappedStore.changeDelegate(store);
        wrappedStore.setValidationEnabled(false);
        context.registerService(DOMDataBroker, newDataBroker, emptyProperties)
        context.registerService(SchemaServiceListener, wrappedStore, emptyProperties)
        context.registerService(SchemaServiceListener, dataService,emptyProperties)
        context.registerService(SchemaServiceListener, operStore, emptyProperties)
        context.registerService(SchemaServiceListener, configStore, emptyProperties)
        

//        dataService.registerConfigurationReader(ROOT, wrappedStore);
//        dataService.registerCommitHandler(ROOT, wrappedStore);
//        dataService.registerOperationalReader(ROOT, wrappedStore);

        mountService = new MountPointManagerImpl();
        mountService.setDataBroker(dataService);

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
