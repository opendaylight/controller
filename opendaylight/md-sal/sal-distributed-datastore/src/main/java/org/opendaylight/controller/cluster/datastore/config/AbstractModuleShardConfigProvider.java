/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import static java.util.Objects.requireNonNull;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractModuleShardConfigProvider implements ModuleShardConfigProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractModuleShardConfigProvider.class);

    static final Config loadConfigFromPath(final String configPath) {
        final File configFile = new File(configPath);
        Config config = null;
        if (configFile.exists()) {
            LOG.info("Config file exists - reading config from it");
            config = ConfigFactory.parseFile(configFile);
        } else {
            LOG.warn("Reading Config from resource");
            config = ConfigFactory.load(configPath);
        }
        return config;
    }

    static final void readModulesConfig(final Config modulesConfig,
            final Map<String, ModuleConfig.Builder> moduleConfigMap, final Configuration configuration) {
        for (final ConfigObject o : modulesConfig.getObjectList("modules")) {
            final ConfigObjectWrapper wrapper = new ConfigObjectWrapper(o);

            final String moduleName = wrapper.stringValue("name");
            final ModuleConfig.Builder builder = moduleConfigMap.computeIfAbsent(moduleName, ModuleConfig::builder);

            builder.nameSpace(wrapper.stringValue("namespace"));
            builder.shardStrategy(ShardStrategyFactory.newShardStrategyInstance(moduleName,
                    wrapper.stringValue("shard-strategy"), configuration));
        }
    }

    static final Map<String, ModuleConfig.Builder> readModuleShardsConfig(final Config moduleShardsConfig) {
        final Map<String, ModuleConfig.Builder> moduleConfigMap = new HashMap<>();
        for (final ConfigObject moduleShardConfigObject : moduleShardsConfig.getObjectList("module-shards")) {
            final String moduleName = moduleShardConfigObject.get("name").unwrapped().toString();
            final ModuleConfig.Builder builder = ModuleConfig.builder(moduleName);

            for (final ConfigObject shard : moduleShardConfigObject.toConfig().getObjectList("shards")) {
                final String shardName = shard.get("name").unwrapped().toString();
                final List<MemberName> replicas = shard.toConfig().getStringList("replicas").stream()
                        .map(MemberName::forName).collect(Collectors.toList());
                builder.shardConfig(shardName, replicas);
            }

            moduleConfigMap.put(moduleName, builder);
        }

        return moduleConfigMap;
    }

    private static final class ConfigObjectWrapper {
        private final ConfigObject configObject;

        ConfigObjectWrapper(final ConfigObject configObject) {
            this.configObject = requireNonNull(configObject);
        }

        String stringValue(final String name) {
            return configObject.get(name).unwrapped().toString();
        }
    }
}
