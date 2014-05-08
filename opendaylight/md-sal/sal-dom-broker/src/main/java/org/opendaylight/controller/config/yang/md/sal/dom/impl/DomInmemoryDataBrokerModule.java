/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.dom.impl;

import java.util.concurrent.Executors;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMDataBrokerImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
*
*/
public final class DomInmemoryDataBrokerModule extends
        org.opendaylight.controller.config.yang.md.sal.dom.impl.AbstractDomInmemoryDataBrokerModule {

    public DomInmemoryDataBrokerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DomInmemoryDataBrokerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final DomInmemoryDataBrokerModule oldModule, final java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
        // Add custom validation for module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        ListeningExecutorService storeExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
        //Initializing Operational DOM DataStore defaulting to InMemoryDOMDataStore if one is not configured
        DOMStore operStore =  getOperationalDataStoreDependency();
        if(operStore == null){
           //we will default to InMemoryDOMDataStore creation
          operStore = new InMemoryDOMDataStore("DOM-OPER", storeExecutor);
          //here we will register the SchemaContext listener
          getSchemaServiceDependency().registerSchemaServiceListener((InMemoryDOMDataStore)operStore);
        }

        DOMStore configStore = getConfigDataStoreDependency();
        if(configStore == null){
           //we will default to InMemoryDOMDataStore creation
           configStore = new InMemoryDOMDataStore("DOM-CFG", storeExecutor);
          //here we will register the SchemaContext listener
          getSchemaServiceDependency().registerSchemaServiceListener((InMemoryDOMDataStore)configStore);
        }
        ImmutableMap<LogicalDatastoreType, DOMStore> datastores = ImmutableMap
                .<LogicalDatastoreType, DOMStore> builder().put(LogicalDatastoreType.OPERATIONAL, operStore)
                .put(LogicalDatastoreType.CONFIGURATION, configStore).build();

        DOMDataBrokerImpl newDataBroker = new DOMDataBrokerImpl(datastores, MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));

        return newDataBroker;
    }
}
