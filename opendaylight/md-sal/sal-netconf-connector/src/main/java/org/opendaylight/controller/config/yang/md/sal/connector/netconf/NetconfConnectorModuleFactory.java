/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.connector.netconf;

import java.io.File;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaContextFactory;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceFilter;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.util.FilesystemSchemaSourceCache;
import org.opendaylight.yangtools.yang.parser.repo.SharedSchemaRepository;
import org.opendaylight.yangtools.yang.parser.util.TextToASTTransformer;
import org.osgi.framework.BundleContext;

/**
*
*/
public class NetconfConnectorModuleFactory extends
        org.opendaylight.controller.config.yang.md.sal.connector.netconf.AbstractNetconfConnectorModuleFactory {

    private static final String SCHEMA_CACHE_DIRECTORY = "cache/schema";
    private final SharedSchemaRepository repository = new SharedSchemaRepository(NAME);
    private final SchemaContextFactory schemaContextFactory
            = repository.createSchemaContextFactory(SchemaSourceFilter.ALWAYS_ACCEPT);

    public NetconfConnectorModuleFactory() {
        // Start cache and Text to AST transformer
        final FilesystemSchemaSourceCache<YangTextSchemaSource> cache = new FilesystemSchemaSourceCache<>(repository, YangTextSchemaSource.class, new File(SCHEMA_CACHE_DIRECTORY));
        repository.registerSchemaSourceListener(cache);
        repository.registerSchemaSourceListener(TextToASTTransformer.create(repository, repository));
    }

    @Override
    public Module createModule(final String instanceName, final DependencyResolver dependencyResolver,
            final DynamicMBeanWithInstance old, final BundleContext bundleContext) throws Exception {
        final NetconfConnectorModule module = (NetconfConnectorModule) super.createModule(instanceName, dependencyResolver,
                old, bundleContext);

        module.setBundleContext(bundleContext);
        module.setSchemaRegistry(repository);
        module.setSchemaContextFactory(schemaContextFactory);
        return module;
    }

    @Override
    public Module createModule(final String instanceName, final DependencyResolver dependencyResolver, final BundleContext bundleContext) {
        final NetconfConnectorModule module = (NetconfConnectorModule) super.createModule(instanceName, dependencyResolver,
                bundleContext);
        module.setBundleContext(bundleContext);
        final String schemaCacheDirectory = module.getSchemaCacheDirectory();
        if(schemaCacheDirectory.equals(SCHEMA_CACHE_DIRECTORY)) {
            module.setSchemaRegistry(repository);
        } else {
            final SharedSchemaRepository deviceRepository = createDeviceRepository(instanceName, schemaCacheDirectory);
            module.setSchemaRegistry(deviceRepository);
        }
        module.setSchemaContextFactory(schemaContextFactory);
        return module;
    }

    private SharedSchemaRepository createDeviceRepository(final String instanceName, final String schemaCacheDirectory) {
        final SharedSchemaRepository deviceRepository = new SharedSchemaRepository(instanceName);
        final SchemaContextFactory deviceSchemaContextFactory =
                deviceRepository.createSchemaContextFactory(SchemaSourceFilter.ALWAYS_ACCEPT);
        final FilesystemSchemaSourceCache<YangTextSchemaSource> deviceCache = new FilesystemSchemaSourceCache<>(repository, YangTextSchemaSource.class, new File(schemaCacheDirectory));
        deviceRepository.registerSchemaSourceListener(deviceCache);
        deviceRepository.registerSchemaSourceListener(TextToASTTransformer.create(deviceRepository, deviceRepository));
        return deviceRepository;
    }
}
