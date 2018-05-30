/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.annotations.VisibleForTesting;
import javassist.ClassPool;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;

/**
 * Factory for creating DatastoreContextIntrospector instances.
 *
 * @author Thomas Pantelis
 */
public class DatastoreContextIntrospectorFactory {
    private final DOMSchemaService schemaService;
    private final ClassLoadingStrategy classLoadingStrategy;

    public DatastoreContextIntrospectorFactory(DOMSchemaService schemaService,
            ClassLoadingStrategy classLoadingStrategy) {
        this.schemaService = schemaService;
        this.classLoadingStrategy = classLoadingStrategy;
    }

    public DatastoreContextIntrospector newInstance(LogicalDatastoreType datastoreType) {
        return new DatastoreContextIntrospector(DatastoreContext.newBuilder()
                .logicalStoreType(datastoreType).tempFileDirectory("./data").build(), newBindingSerializer());
    }

    @VisibleForTesting
    DatastoreContextIntrospector newInstance(DatastoreContext context) {
        return new DatastoreContextIntrospector(context, newBindingSerializer());
    }

    private BindingNormalizedNodeSerializer newBindingSerializer() {
        BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(
                StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault())));
        codecRegistry.onBindingRuntimeContextUpdated(BindingRuntimeContext.create(classLoadingStrategy,
                schemaService.getGlobalContext()));
        return codecRegistry;
    }
}
