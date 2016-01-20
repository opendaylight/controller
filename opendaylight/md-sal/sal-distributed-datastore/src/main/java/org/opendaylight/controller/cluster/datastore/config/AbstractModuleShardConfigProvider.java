/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import com.google.common.base.Verify;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;

public abstract class AbstractModuleShardConfigProvider implements ModuleShardConfigProvider {

    protected abstract Config loadModuleShardsConfig();
    protected abstract Config loadModulesConfig();

    @Override
    public final Map<String, ModuleConfig.Builder> retrieveModuleConfigs(Configuration configuration) {
        Config moduleShardsConfig = Verify.verifyNotNull(loadModuleShardsConfig());
        Config modulesConfig = Verify.verifyNotNull(loadModulesConfig());

        Map<String, ModuleConfig.Builder> moduleConfigMap = readModuleShardsConfig(moduleShardsConfig);
        readModulesConfig(modulesConfig, moduleConfigMap, configuration);

        return moduleConfigMap;
    }

    private static void readModulesConfig(final Config modulesConfig, Map<String, ModuleConfig.Builder> moduleConfigMap,
            Configuration configuration) {
        List<? extends ConfigObject> modulesConfigObjectList = modulesConfig.getObjectList("modules");

        for(ConfigObject o : modulesConfigObjectList){
            ConfigObjectWrapper w = new ConfigObjectWrapper(o);

            String moduleName = w.stringValue("name");
            ModuleConfig.Builder builder = moduleConfigMap.get(moduleName);
            if(builder == null) {
                builder = ModuleConfig.builder(moduleName);
                moduleConfigMap.put(moduleName, builder);
            }

            builder.nameSpace(w.stringValue("namespace"));
            builder.shardStrategy(ShardStrategyFactory.newShardStrategyInstance(moduleName,
                    w.stringValue("shard-strategy"), configuration));
        }
    }

    private static Map<String, ModuleConfig.Builder> readModuleShardsConfig(final Config moduleShardsConfig) {
        List<? extends ConfigObject> moduleShardsConfigObjectList =
            moduleShardsConfig.getObjectList("module-shards");

        Map<String, ModuleConfig.Builder> moduleConfigMap = new HashMap<>();
        for(ConfigObject moduleShardConfigObject : moduleShardsConfigObjectList){
            String moduleName = moduleShardConfigObject.get("name").unwrapped().toString();
            ModuleConfig.Builder builder = ModuleConfig.builder(moduleName);

            List<? extends ConfigObject> shardsConfigObjectList =
                moduleShardConfigObject.toConfig().getObjectList("shards");

            for(ConfigObject shard : shardsConfigObjectList){
                String shardName = shard.get("name").unwrapped().toString();
                List<String> replicas = shard.toConfig().getStringList("replicas");
                builder.shardConfig(shardName, replicas);
            }

            moduleConfigMap.put(moduleName, builder);
        }

        return moduleConfigMap;
    }

    private static class ConfigObjectWrapper{

        private final ConfigObject configObject;

        ConfigObjectWrapper(final ConfigObject configObject){
            this.configObject = configObject;
        }

        public String stringValue(final String name){
            return configObject.get(name).unwrapped().toString();
        }
    }
}
