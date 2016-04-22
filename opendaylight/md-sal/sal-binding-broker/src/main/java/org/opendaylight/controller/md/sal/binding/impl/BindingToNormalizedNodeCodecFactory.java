/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.api.ClassLoadingStrategy;

/**
 * Factory class for creating and initializing the global BindingToNormalizedNodeCodec instance.
 *
 * @author Thomas Pantelis
 */
public class BindingToNormalizedNodeCodecFactory {
    private static final AtomicBoolean INSTANCE_CREATED = new AtomicBoolean();
    private static volatile BindingToNormalizedNodeCodec instance;

    /**
     * Returns the global BindingToNormalizedNodeCodec instance, creating if necessary. The returned instance
     * is registered with tthe SchemaService as a SchemaContextListener.
     *
     * @param classLoadingStrategy
     * @param schemaService
     * @return the BindingToNormalizedNodeCodec instance
     */
    public static BindingToNormalizedNodeCodec getOrCreateInstance(ClassLoadingStrategy classLoadingStrategy,
            SchemaService schemaService) {
        if(!INSTANCE_CREATED.compareAndSet(false, true)) {
            return instance;
        }

        BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(
                StreamWriterGenerator.create(SingletonHolder.JAVASSIST));
        BindingToNormalizedNodeCodec localInstance = new BindingToNormalizedNodeCodec(
                classLoadingStrategy, codecRegistry, true);

        schemaService.registerSchemaContextListener(localInstance);

        // Publish the BindingToNormalizedNodeCodec instance after we've registered it as a
        // SchemaContextListener to avoid a race condition by publishing it too early when it isn't
        // fully initialized.
        instance = localInstance;
        return instance;
    }

    public static BindingToNormalizedNodeCodec getInstance() {
        return instance;
    }
}
