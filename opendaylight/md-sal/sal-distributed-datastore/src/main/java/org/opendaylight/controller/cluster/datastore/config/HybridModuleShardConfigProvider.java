/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import com.typesafe.config.Config;
import java.util.Map;

public class HybridModuleShardConfigProvider extends AbstractModuleShardConfigProvider {

    private Config moduleShardsConfig;
    private String modulesConfigPath;

    public HybridModuleShardConfigProvider(final Config moduleShardsConfig, final String modulesConfigPath) {
        this.moduleShardsConfig = moduleShardsConfig;
        this.modulesConfigPath = modulesConfigPath;
    }

    @Override
    public Map<String, ModuleConfig.Builder> retrieveModuleConfigs(final Configuration configuration) {
        Config modulesConfig = loadConfigFromPath(modulesConfigPath);
        final Map<String, ModuleConfig.Builder> moduleConfigMap = readModuleShardsConfig(this.moduleShardsConfig);
        readModulesConfig(modulesConfig, moduleConfigMap, configuration);

        return moduleConfigMap;
    }
}
