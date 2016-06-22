/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.sal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

/**
 * Factory class for creating and initializing the BindingToNormalizedNodeCodec instances.
 *
 * @author Thomas Pantelis
 */
public class BindingToNormalizedNodeCodecFactory {
    /**
     * This method is deprecated in favor of newInstance/registerInstance.
     *
     * @param classLoadingStrategy
     * @param schemaService
     * @return BindingToNormalizedNodeCodec instance
     */
    @Deprecated
    public static BindingToNormalizedNodeCodec getOrCreateInstance(ClassLoadingStrategy classLoadingStrategy,
                            SchemaService schemaService) {
        BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(
                StreamWriterGenerator.create(SingletonHolder.JAVASSIST));
        BindingToNormalizedNodeCodec instance = new BindingToNormalizedNodeCodec(
                               classLoadingStrategy, codecRegistry, true);
        schemaService.registerSchemaContextListener(instance);
        return instance;
    }

    /**
     * Creates a new BindingToNormalizedNodeCodec instance.
     *
     * @param classLoadingStrategy
     * @return the BindingToNormalizedNodeCodec instance
     */
    public static BindingToNormalizedNodeCodec newInstance(ClassLoadingStrategy classLoadingStrategy) {
        BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(
                StreamWriterGenerator.create(SingletonHolder.JAVASSIST));
        return new BindingToNormalizedNodeCodec(classLoadingStrategy, codecRegistry, true);
    }

    /**
     * Registers the given instance with the SchemaService as a SchemaContextListener.
     *
     * @param instance the BindingToNormalizedNodeCodec instance
     * @param schemaService the SchemaService.
     * @return the ListenerRegistration
     */
    public static ListenerRegistration<SchemaContextListener> registerInstance(BindingToNormalizedNodeCodec instance,
            SchemaService schemaService) {
        return schemaService.registerSchemaContextListener(instance);
    }
}
