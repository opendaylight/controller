/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import com.typesafe.config.Config;
import java.util.Map;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ModuleShardConfigProvider that reads the module and shard configuration from files.
 *
 * @author Thomas Pantelis
 */
@Component(immediate = true, service = ModuleShardConfigProvider.class)
public class FileModuleShardConfigProvider extends AbstractModuleShardConfigProvider {
    private static final Logger LOG = LoggerFactory.getLogger(FileModuleShardConfigProvider.class);

    private final String moduleShardsConfigPath;
    private final String modulesConfigPath;

    public FileModuleShardConfigProvider() {
        this("./configuration/initial/module-shards.conf", "./configuration/initial/modules.conf");
    }

    public FileModuleShardConfigProvider(final String moduleShardsConfigPath, final String modulesConfigPath) {
        this.moduleShardsConfigPath = moduleShardsConfigPath;
        this.modulesConfigPath = modulesConfigPath;
    }

    @Override
    public Map<String, ModuleConfig.Builder> retrieveModuleConfigs(final Configuration configuration) {
        Config moduleShardsConfig = loadConfigFromPath(moduleShardsConfigPath);
        Config modulesConfig = loadConfigFromPath(modulesConfigPath);

        final Map<String, ModuleConfig.Builder> moduleConfigMap = readModuleShardsConfig(moduleShardsConfig);
        readModulesConfig(modulesConfig, moduleConfigMap, configuration);
        return moduleConfigMap;
    }

    @Activate
    void activate() {
        LOG.info("Shard configuration provider started");
    }

    @Deactivate
    void deactivate() {
        LOG.info("Shard configuration provider stopped");
    }
}
