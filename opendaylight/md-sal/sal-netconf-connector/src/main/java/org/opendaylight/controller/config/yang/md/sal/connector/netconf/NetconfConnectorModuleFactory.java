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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
*
*/
public class NetconfConnectorModuleFactory extends
        org.opendaylight.controller.config.yang.md.sal.connector.netconf.AbstractNetconfConnectorModuleFactory {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfConnectorModuleFactory.class);
    private static final String CACHE_DIRECTORY = "cache";
    private static final String SCHEMA_DIRECTORY = "schema";
    private static final String SCHEMA_CACHE_DIRECTORY = CACHE_DIRECTORY + File.separator + SCHEMA_DIRECTORY;
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
        updateSchemaCacheDirectory(instanceName, module);
        repository.registerSchemaSourceListener(TextToASTTransformer.create(repository, repository));
        return module;
    }

    @Override
    public Module createModule(final String instanceName, final DependencyResolver dependencyResolver, final BundleContext bundleContext) {

        final NetconfConnectorModule module = (NetconfConnectorModule) super.createModule(instanceName, dependencyResolver,
                bundleContext);
        module.setBundleContext(bundleContext);
        updateSchemaCacheDirectory(instanceName, module);
        repository.registerSchemaSourceListener(TextToASTTransformer.create(repository, repository));
        return module;
    }

    /**
     * If a custom <code>schemaCacheDirectory</code> is specified, this method sets up the custom location.
     * Otherwise, the default <code>cache/schema</code> is assumed.
     *
     * @param instanceName The name specified for the netconf mount point
     * @param module The module that is actively being created.
     */
    private void updateSchemaCacheDirectory(final String instanceName, final NetconfConnectorModule module) {

        module.setSchemaRegistry(repository);
        module.setSchemaContextFactory(schemaContextFactory);

        final String schemaCacheDirectory = module.getSchemaCacheDirectory();
        // Checks for a custom schema cache directory
        if(schemaCacheDirectory != null && !schemaCacheDirectory.equals(SCHEMA_DIRECTORY)) {
            final FilesystemSchemaSourceCache<YangTextSchemaSource> deviceCache = createDeviceFilesystemCache(schemaCacheDirectory);
            repository.registerSchemaSourceListener(deviceCache);
            LOG.info("Netconf connector for device {} will use schema cache directory {} instead of {}",
                    instanceName, schemaCacheDirectory, SCHEMA_CACHE_DIRECTORY);
        }
    }

    /**
     * Creates a <code>FilesystemSchemaSourceCache</code> for the custom schema cache directory.
     *
     * @param schemaCacheDirectory The custom cache directory relative to "cache"
     * @return A <code>FilesystemSchemaSourceCache</code> for the custom schema cache directory
     */
    private FilesystemSchemaSourceCache<YangTextSchemaSource> createDeviceFilesystemCache(final String schemaCacheDirectory) {
        final String relativeSchemaCacheDirectory = CACHE_DIRECTORY + File.separator + schemaCacheDirectory;
        return new FilesystemSchemaSourceCache<>(repository, YangTextSchemaSource.class, new File(relativeSchemaCacheDirectory));
    }
}
