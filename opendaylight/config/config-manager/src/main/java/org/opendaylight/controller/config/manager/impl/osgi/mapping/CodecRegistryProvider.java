/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi.mapping;

import javassist.ClassPool;
import org.opendaylight.controller.config.manager.impl.util.OsgiRegistrationUtil;
import org.opendaylight.yangtools.sal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.impl.RuntimeGeneratedMappingServiceImpl;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.osgi.framework.BundleContext;

/**
 * Creates and initializes {@link RuntimeGeneratedMappingServiceImpl}, which is used to get {@link CodecRegistry}.
 * Also maintains service registrations of {@link RuntimeGeneratedMappingServiceImpl}.
 */
// TODO move to yang runtime
public class CodecRegistryProvider implements AutoCloseable {
    private static final ClassPool CLASS_POOL = ClassPool.getDefault();

    private final RuntimeGeneratedMappingServiceImpl service;
    private final AutoCloseable registration;

    public CodecRegistryProvider(final ClassLoadingStrategy classLoadingStrategy, final BundleContext context) {
        service = new RuntimeGeneratedMappingServiceImpl(CLASS_POOL, classLoadingStrategy);
        registration = OsgiRegistrationUtil.registerService(context, service,
                SchemaContextListener.class, BindingIndependentMappingService.class);
    }

    public CodecRegistry getCodecRegistry() {
        return service.getCodecRegistry();
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }
}
