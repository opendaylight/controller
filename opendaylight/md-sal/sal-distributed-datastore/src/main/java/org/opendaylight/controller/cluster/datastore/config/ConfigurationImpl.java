/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;

// FIXME: Non-final for testing
public class ConfigurationImpl implements Configuration {
    private volatile Map<String, ModuleConfig> moduleConfigMap;

    // Look up maps to speed things up

    private volatile Map<String, String> namespaceToModuleName;
    private volatile Set<String> allShardNames;

    public ConfigurationImpl(final String moduleShardsConfigPath, final String modulesConfigPath) {
        this(new FileModuleShardConfigProvider(moduleShardsConfigPath, modulesConfigPath));
    }

    public ConfigurationImpl(final ModuleShardConfigProvider provider) {
        ImmutableMap.Builder<String, ModuleConfig> mapBuilder = ImmutableMap.builder();
        for (Entry<String, ModuleConfig.Builder> e: provider.retrieveModuleConfigs(this).entrySet()) {
            mapBuilder.put(e.getKey(), e.getValue().build());
        }

        moduleConfigMap = mapBuilder.build();

        allShardNames = createAllShardNames(moduleConfigMap.values());
        namespaceToModuleName = createNamespaceToModuleName(moduleConfigMap.values());
    }

    private static Set<String> createAllShardNames(final Iterable<ModuleConfig> moduleConfigs) {
        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (ModuleConfig moduleConfig : moduleConfigs) {
            builder.addAll(moduleConfig.getShardNames());
        }

        return builder.build();
    }

    private static Map<String, String> createNamespaceToModuleName(final Iterable<ModuleConfig> moduleConfigs) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (ModuleConfig moduleConfig : moduleConfigs) {
            if (moduleConfig.getNamespace() != null) {
                builder.put(moduleConfig.getNamespace(), moduleConfig.getName());
            }
        }

        return builder.build();
    }

    @Override
    public Collection<String> getMemberShardNames(final MemberName memberName) {
        requireNonNull(memberName, "memberName should not be null");

        List<String> shards = new ArrayList<>();
        for (ModuleConfig moduleConfig: moduleConfigMap.values()) {
            for (ShardConfig shardConfig: moduleConfig.getShardConfigs()) {
                if (shardConfig.getReplicas().contains(memberName)) {
                    shards.add(shardConfig.getName());
                }
            }
        }

        return shards;
    }

    @Override
    public String getModuleNameFromNameSpace(final String nameSpace) {
        return namespaceToModuleName.get(requireNonNull(nameSpace, "nameSpace should not be null"));
    }

    @Override
    public ShardStrategy getStrategyForModule(final String moduleName) {
        ModuleConfig moduleConfig = getModuleConfig(moduleName);
        return moduleConfig != null ? moduleConfig.getShardStrategy() : null;
    }

    @Override
    public String getShardNameForModule(final String moduleName) {
        ModuleConfig moduleConfig = getModuleConfig(moduleName);
        if (moduleConfig != null) {
            Collection<ShardConfig> shardConfigs = moduleConfig.getShardConfigs();
            if (!shardConfigs.isEmpty()) {
                return shardConfigs.iterator().next().getName();
            }
        }
        return null;
    }

    private ModuleConfig getModuleConfig(final String moduleName) {
        return moduleConfigMap.get(requireNonNull(moduleName, "moduleName should not be null"));
    }

    @Override
    public Collection<MemberName> getMembersFromShardName(final String shardName) {
        checkNotNullShardName(shardName);

        for (ModuleConfig moduleConfig: moduleConfigMap.values()) {
            ShardConfig shardConfig = moduleConfig.getShardConfig(shardName);
            if (shardConfig != null) {
                return shardConfig.getReplicas();
            }
        }

        return List.of();
    }

    private static void checkNotNullShardName(final String shardName) {
        requireNonNull(shardName, "shardName should not be null");
    }

    @Override
    public Set<String> getAllShardNames() {
        return allShardNames;
    }

    @Override
    public Collection<MemberName> getUniqueMemberNamesForAllShards() {
        Set<MemberName> allNames = new HashSet<>();
        for (String shardName: getAllShardNames()) {
            allNames.addAll(getMembersFromShardName(shardName));
        }

        return allNames;
    }

    @Override
    public synchronized void addModuleShardConfiguration(final ModuleShardConfiguration config) {
        requireNonNull(config, "ModuleShardConfiguration should not be null");

        ModuleConfig moduleConfig = ModuleConfig.builder(config.getModuleName())
                .nameSpace(config.getNamespace().toString())
                .shardStrategy(createShardStrategy(config.getModuleName(), config.getShardStrategyName()))
                .shardConfig(config.getShardName(), config.getShardMemberNames()).build();

        updateModuleConfigMap(moduleConfig);

        namespaceToModuleName = ImmutableMap.<String, String>builder().putAll(namespaceToModuleName)
                .put(moduleConfig.getNamespace(), moduleConfig.getName()).build();
        allShardNames = ImmutableSet.<String>builder().addAll(allShardNames).add(config.getShardName()).build();
    }

    private ShardStrategy createShardStrategy(final String moduleName, final String shardStrategyName) {
        return ShardStrategyFactory.newShardStrategyInstance(moduleName, shardStrategyName, this);
    }

    @Override
    public boolean isShardConfigured(final String shardName) {
        checkNotNullShardName(shardName);
        return allShardNames.contains(shardName);
    }

    @Override
    public void addMemberReplicaForShard(final String shardName, final MemberName newMemberName) {
        checkNotNullShardName(shardName);
        requireNonNull(newMemberName, "MemberName should not be null");

        for (ModuleConfig moduleConfig: moduleConfigMap.values()) {
            ShardConfig shardConfig = moduleConfig.getShardConfig(shardName);
            if (shardConfig != null) {
                Set<MemberName> replicas = new HashSet<>(shardConfig.getReplicas());
                replicas.add(newMemberName);
                updateModuleConfigMap(ModuleConfig.builder(moduleConfig).shardConfig(shardName, replicas).build());
                return;
            }
        }
    }

    @Override
    public void removeMemberReplicaForShard(final String shardName, final MemberName newMemberName) {
        checkNotNullShardName(shardName);
        requireNonNull(newMemberName, "MemberName should not be null");

        for (ModuleConfig moduleConfig: moduleConfigMap.values()) {
            ShardConfig shardConfig = moduleConfig.getShardConfig(shardName);
            if (shardConfig != null) {
                Set<MemberName> replicas = new HashSet<>(shardConfig.getReplicas());
                replicas.remove(newMemberName);
                updateModuleConfigMap(ModuleConfig.builder(moduleConfig).shardConfig(shardName, replicas).build());
                return;
            }
        }
    }

    private void updateModuleConfigMap(final ModuleConfig moduleConfig) {
        final Map<String, ModuleConfig> newModuleConfigMap = new HashMap<>(moduleConfigMap);
        newModuleConfigMap.put(moduleConfig.getName(), moduleConfig);
        moduleConfigMap = ImmutableMap.copyOf(newModuleConfigMap);
    }
}
