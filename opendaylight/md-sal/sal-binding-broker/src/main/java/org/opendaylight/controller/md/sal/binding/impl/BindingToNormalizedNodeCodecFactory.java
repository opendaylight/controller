/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import java.util.Hashtable;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.osgi.framework.BundleContext;

/**
 * Factory class for creating and initializing a BindingToNormalizedNodeCodec instance.
 *
 * @author Thomas Pantelis
 */
public class BindingToNormalizedNodeCodecFactory {
    /**
     * Creates a BindingToNormalizedNodeCodec instance. The returned instance is registered with the
     * bundleContext as a SchemaContextListener and it waits a period of time to ensure the instance has a
     * valid SchemaContext instance.
     *
     * @param classLoadingStrategy
     * @param bundleContext
     * @return a BindingToNormalizedNodeCodec instance
     */
    public static BindingToNormalizedNodeCodec newInstance(ClassLoadingStrategy classLoadingStrategy,
            BundleContext bundleContext) {
        BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(
                StreamWriterGenerator.create(SingletonHolder.JAVASSIST));
        BindingToNormalizedNodeCodec instance = new BindingToNormalizedNodeCodec(classLoadingStrategy,
                codecRegistry, true);

        bundleContext.registerService(SchemaContextListener.class, instance, new Hashtable<String,String>());
        return instance;
    }
}
