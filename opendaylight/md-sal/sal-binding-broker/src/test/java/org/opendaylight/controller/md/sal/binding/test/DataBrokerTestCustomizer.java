/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test;

import javassist.ClassPool;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.impl.ForwardedBindingDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMDataBrokerImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.binding.test.util.MockSchemaService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.yangtools.sal.binding.generator.impl.RuntimeGeneratedMappingServiceImpl;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class DataBrokerTestCustomizer {

    private DOMDataBroker domDataBroker;
    private final RuntimeGeneratedMappingServiceImpl mappingService;
    private final MockSchemaService schemaService;
    private ImmutableMap<LogicalDatastoreType, DOMStore> datastores;

    public ImmutableMap<LogicalDatastoreType, DOMStore> createDatastores() {
        return ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                .put(LogicalDatastoreType.OPERATIONAL, createOperationalDatastore())
                .put(LogicalDatastoreType.CONFIGURATION,createConfigurationDatastore())
                .build();
    }

    public DataBrokerTestCustomizer() {
        schemaService = new MockSchemaService();
        mappingService = new RuntimeGeneratedMappingServiceImpl(ClassPool.getDefault());
    }

    public DOMStore createConfigurationDatastore() {
        InMemoryDOMDataStore store = new InMemoryDOMDataStore("CFG", MoreExecutors.sameThreadExecutor());
        schemaService.registerSchemaServiceListener(store);
        return store;
    }

    public DOMStore createOperationalDatastore() {
        InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", MoreExecutors.sameThreadExecutor());
        schemaService.registerSchemaServiceListener(store);
        return store;
    }

    public DOMDataBroker createDOMDataBroker() {
        return new DOMDataBrokerImpl(getDatastores(), getCommitCoordinatorExecutor());
    }

    public ListeningExecutorService getCommitCoordinatorExecutor() {
        return MoreExecutors.sameThreadExecutor();
    }

    public DataBroker createDataBroker() {
        return new ForwardedBindingDataBroker(getDOMDataBroker(), getMappingService(), getSchemaService());
    }

    private SchemaService getSchemaService() {
        return schemaService;
    }

    private BindingIndependentMappingService getMappingService() {
        return mappingService;
    }

    private DOMDataBroker getDOMDataBroker() {
        if(domDataBroker == null) {
            domDataBroker = createDOMDataBroker();
        }
        return domDataBroker;
    }

    private ImmutableMap<LogicalDatastoreType, DOMStore> getDatastores() {
        if(datastores == null) {
            datastores = createDatastores();
        }
        return datastores;
    }

    public void updateSchema(final SchemaContext ctx) {
        schemaService.changeSchema(ctx);
        mappingService.onGlobalContextUpdated(ctx);
    }

}
