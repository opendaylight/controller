/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.shard.configuration.rev191128.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.shard.configuration.rev191128.shard.persistence.Persistence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.shard.configuration.rev191128.shard.persistence.PersistenceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ModuleShardConfigProvider that reads the module and shard configuration from files.
 *
 * @author Thomas Pantelis
 */
public class FileModuleShardConfigProvider implements ModuleShardConfigProvider {
    private static final Logger LOG = LoggerFactory.getLogger(FileModuleShardConfigProvider.class);

    private final String moduleShardsConfigPath;
    private final String modulesConfigPath;

    public FileModuleShardConfigProvider(final String moduleShardsConfigPath, final String modulesConfigPath) {
        this.moduleShardsConfigPath = moduleShardsConfigPath;
        this.modulesConfigPath = modulesConfigPath;
    }

    @Override
    public Map<String, ModuleConfig.Builder> retrieveModuleConfigs(final Configuration configuration) {
        final File moduleShardsFile = new File(moduleShardsConfigPath);
        final File modulesFile = new File(modulesConfigPath);

        Config moduleShardsConfig = null;
        if (moduleShardsFile.exists()) {
            LOG.info("module shards config file exists - reading config from it");
            moduleShardsConfig = ConfigFactory.parseFile(moduleShardsFile);
        } else {
            LOG.warn("module shards configuration read from resource");
            moduleShardsConfig = ConfigFactory.load(moduleShardsConfigPath);
        }

        Config modulesConfig = null;
        if (modulesFile.exists()) {
            LOG.info("modules config file exists - reading config from it");
            modulesConfig = ConfigFactory.parseFile(modulesFile);
        } else {
            LOG.warn("modules configuration read from resource");
            modulesConfig = ConfigFactory.load(modulesConfigPath);
        }

        final Map<String, ModuleConfig.Builder> moduleConfigMap = readModuleShardsConfig(moduleShardsConfig);
        readModulesConfig(modulesConfig, moduleConfigMap, configuration);

        return moduleConfigMap;
    }

    private static void readModulesConfig(final Config modulesConfig,
            final Map<String, ModuleConfig.Builder> moduleConfigMap, final Configuration configuration) {
        final List<? extends ConfigObject> modulesConfigObjectList = modulesConfig.getObjectList("modules");

        for (final ConfigObject o : modulesConfigObjectList) {
            final ConfigObjectWrapper wrapper = new ConfigObjectWrapper(o);

            final String moduleName = wrapper.stringValue("name");
            final ModuleConfig.Builder builder = moduleConfigMap.computeIfAbsent(moduleName, ModuleConfig::builder);

            builder.nameSpace(wrapper.stringValue("namespace"));
            builder.shardStrategy(ShardStrategyFactory.newShardStrategyInstance(moduleName,
                    wrapper.stringValue("shard-strategy"), configuration));
        }
    }

    private static Map<String, ModuleConfig.Builder> readModuleShardsConfig(final Config moduleShardsConfig) {
        final List<? extends ConfigObject> moduleShardsConfigObjectList =
            moduleShardsConfig.getObjectList("module-shards");

        final Map<String, ModuleConfig.Builder> moduleConfigMap = new HashMap<>();
        for (final ConfigObject moduleShardConfigObject : moduleShardsConfigObjectList) {
            final String moduleName = moduleShardConfigObject.get("name").unwrapped().toString();
            Persistence persistence = null;
            if (moduleShardConfigObject.containsKey("persistence")) {
                ConfigObject persistenceObject = moduleShardConfigObject.toConfig().getObject("persistence");
                if (persistenceObject.containsKey("datastore-type") && persistenceObject.containsKey("persistent")) {
                    String datastore = persistenceObject.get("datastore-type").unwrapped().toString();
                    Boolean persistent = Boolean.valueOf(persistenceObject.get("persistent").unwrapped().toString());
                    persistence = new PersistenceBuilder().setDatastore(DatastoreType.forName(datastore).get())
                            .setPersistent(persistent).build();
                } else {
                    throw new IllegalStateException("Module-Shard persistence is configured incorrectly");
                }
            }

            final ModuleConfig.Builder builder = ModuleConfig.builder(moduleName);

            final List<? extends ConfigObject> shardsConfigObjectList =
                moduleShardConfigObject.toConfig().getObjectList("shards");

            for (final ConfigObject shard : shardsConfigObjectList) {
                final String shardName = shard.get("name").unwrapped().toString();
                final List<MemberName> replicas = shard.toConfig().getStringList("replicas").stream()
                        .map(MemberName::forName).collect(Collectors.toList());
                builder.shardConfig(shardName, persistence, replicas);
            }

            moduleConfigMap.put(moduleName, builder);
        }

        return moduleConfigMap;
    }

    private static class ConfigObjectWrapper {

        private final ConfigObject configObject;

        ConfigObjectWrapper(final ConfigObject configObject) {
            this.configObject = configObject;
        }

        public String stringValue(final String name) {
            return configObject.get(name).unwrapped().toString();
        }
    }
}
