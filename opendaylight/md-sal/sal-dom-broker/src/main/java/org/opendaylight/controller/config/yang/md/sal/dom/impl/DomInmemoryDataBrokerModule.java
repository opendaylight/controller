/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.dom.impl;

import java.util.Hashtable;
import java.util.concurrent.Executors;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMDataBrokerImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.osgi.framework.BundleContext;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
*
*/
public final class DomInmemoryDataBrokerModule extends
        org.opendaylight.controller.config.yang.md.sal.dom.impl.AbstractDomInmemoryDataBrokerModule {

    private BundleContext bundleContext;

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
        InMemoryDOMDataStore operStore = new InMemoryDOMDataStore("DOM-OPER", storeExecutor);
        InMemoryDOMDataStore configStore = new InMemoryDOMDataStore("DOM-CFG", storeExecutor);
        ImmutableMap<LogicalDatastoreType, DOMStore> datastores = ImmutableMap
                .<LogicalDatastoreType, DOMStore> builder().put(LogicalDatastoreType.OPERATIONAL, operStore)
                .put(LogicalDatastoreType.CONFIGURATION, configStore).build();

        DOMDataBrokerImpl newDataBroker = new DOMDataBrokerImpl(datastores, MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));

        getBundleContext().registerService(DOMDataBroker.class, newDataBroker, new Hashtable<String, String>());

        getSchemaServiceDependency().registerSchemaServiceListener(operStore);
        getSchemaServiceDependency().registerSchemaServiceListener(configStore);

        return newDataBroker;
    }

    private BundleContext getBundleContext() {
        return bundleContext;
    }

    void setBundleContext(final BundleContext ctx) {
        bundleContext = ctx;
    }
}
