/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMDataBrokerAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationPublishServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter;
import org.opendaylight.controller.md.sal.dom.broker.impl.SerializedDOMDataBroker;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.binding.test.util.MockSchemaService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import javassist.ClassPool;

public abstract class AbstractDataBrokerTestCustomizer {

    private DOMDataBroker domDataBroker;
    private final DOMNotificationRouter domNotificationRouter;
    private final MockSchemaService schemaService;
    private ImmutableMap<LogicalDatastoreType, DOMStore> datastores;
    private final BindingToNormalizedNodeCodec bindingToNormalized;

    public ImmutableMap<LogicalDatastoreType, DOMStore> createDatastores() {
        return ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                .put(LogicalDatastoreType.OPERATIONAL, createOperationalDatastore())
                .put(LogicalDatastoreType.CONFIGURATION,createConfigurationDatastore())
                .build();
    }

    public AbstractDataBrokerTestCustomizer() {
        this.schemaService = new MockSchemaService();
        final ClassPool pool = ClassPool.getDefault();
        final DataObjectSerializerGenerator generator = StreamWriterGenerator.create(JavassistUtils.forClassPool(pool));
        final BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(generator);
        final GeneratedClassLoadingStrategy loading = GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy();
        this.bindingToNormalized = new BindingToNormalizedNodeCodec(loading, codecRegistry);
        this.schemaService.registerSchemaContextListener(this.bindingToNormalized);
        this.domNotificationRouter = DOMNotificationRouter.create(16);
    }

    public DOMStore createConfigurationDatastore() {
        final InMemoryDOMDataStore store = new InMemoryDOMDataStore("CFG", MoreExecutors.newDirectExecutorService());
        this.schemaService.registerSchemaContextListener(store);
        return store;
    }

    public DOMStore createOperationalDatastore() {
        final InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", MoreExecutors.newDirectExecutorService());
        this.schemaService.registerSchemaContextListener(store);
        return store;
    }

    public DOMDataBroker createDOMDataBroker() {
        return new SerializedDOMDataBroker(getDatastores(), getCommitCoordinatorExecutor());
    }

    public NotificationService createNotificationService() {
        return new BindingDOMNotificationServiceAdapter(this.bindingToNormalized.getCodecRegistry(), this.domNotificationRouter);
    }

    public NotificationPublishService createNotificationPublishService() {
        return new BindingDOMNotificationPublishServiceAdapter(this.bindingToNormalized, this.domNotificationRouter);
    }

    public abstract ListeningExecutorService getCommitCoordinatorExecutor();

    public DataBroker createDataBroker() {
        return new BindingDOMDataBrokerAdapter(getDOMDataBroker(), this.bindingToNormalized);
    }

    public BindingToNormalizedNodeCodec getBindingToNormalized() {
        return this.bindingToNormalized;
    }

    public SchemaService getSchemaService() {
        return this.schemaService;
    }

    private DOMDataBroker getDOMDataBroker() {
        if (this.domDataBroker == null) {
            this.domDataBroker = createDOMDataBroker();
        }
        return this.domDataBroker;
    }

    private synchronized ImmutableMap<LogicalDatastoreType, DOMStore> getDatastores() {
        if (this.datastores == null) {
            this.datastores = createDatastores();
        }
        return this.datastores;
    }

    public void updateSchema(final SchemaContext ctx) {
        this.schemaService.changeSchema(ctx);
    }

    public DOMNotificationRouter getDomNotificationRouter() {
        return this.domNotificationRouter;
    }
}
