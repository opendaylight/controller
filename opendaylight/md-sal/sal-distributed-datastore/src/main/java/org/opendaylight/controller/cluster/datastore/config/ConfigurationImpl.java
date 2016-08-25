/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.PrefixShardConfiguration;
import org.opendaylight.controller.cluster.datastore.PrefixShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

// TODO clean this up once we get rid of module based configuration, prefix one should be alot simpler
public class ConfigurationImpl implements Configuration {
    private volatile Map<String, ModuleConfig> moduleConfigMap;

    // TODO should this be initialized with something? on restart we should restore the shards from configuration?
    private volatile Map<YangInstanceIdentifier, PrefixShardConfiguration> prefixConfigMap = Collections.emptyMap();

    // Look up maps to speed things up

    private volatile Map<String, String> namespaceToModuleName;
    private volatile Set<String> allShardNames;

    public ConfigurationImpl(final String moduleShardsConfigPath, final String modulesConfigPath) {
        this(new FileModuleShardConfigProvider(moduleShardsConfigPath, modulesConfigPath));
    }

    public ConfigurationImpl(final ModuleShardConfigProvider provider) {
        ImmutableMap.Builder<String, ModuleConfig> mapBuilder = ImmutableMap.builder();
        for(Map.Entry<String, ModuleConfig.Builder> e: provider.retrieveModuleConfigs(this).entrySet()) {
            mapBuilder.put(e.getKey(), e.getValue().build());
        }

        this.moduleConfigMap = mapBuilder.build();

        this.allShardNames = createAllShardNames(moduleConfigMap.values());
        this.namespaceToModuleName = createNamespaceToModuleName(moduleConfigMap.values());
    }

    private static Set<String> createAllShardNames(Iterable<ModuleConfig> moduleConfigs) {
        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for(ModuleConfig moduleConfig : moduleConfigs) {
            builder.addAll(moduleConfig.getShardNames());
        }

        return builder.build();
    }

    private static Map<String, String> createNamespaceToModuleName(Iterable<ModuleConfig> moduleConfigs) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for(ModuleConfig moduleConfig : moduleConfigs) {
            if(moduleConfig.getNameSpace() != null) {
                builder.put(moduleConfig.getNameSpace(), moduleConfig.getName());
            }
        }

        return builder.build();
    }

    @Override
    public Collection<String> getMemberShardNames(final MemberName memberName){
        Preconditions.checkNotNull(memberName, "memberName should not be null");

        List<String> shards = new ArrayList<>();
        for (ModuleConfig moduleConfig: moduleConfigMap.values()) {
            for (ShardConfig shardConfig: moduleConfig.getShardConfigs()) {
                if(shardConfig.getReplicas().contains(memberName)) {
                    shards.add(shardConfig.getName());
                }
            }
        }

        return shards;
    }

    @Override
    public String getModuleNameFromNameSpace(final String nameSpace) {
        Preconditions.checkNotNull(nameSpace, "nameSpace should not be null");

        return namespaceToModuleName.get(nameSpace);
    }

    @Override
    public ShardStrategy getStrategyForModule(String moduleName) {
        Preconditions.checkNotNull(moduleName, "moduleName should not be null");

        ModuleConfig moduleConfig = moduleConfigMap.get(moduleName);
        return moduleConfig != null ? moduleConfig.getShardStrategy(): null;
    }

    @Override
    public String getShardNameForModule(final String moduleName) {
        Preconditions.checkNotNull(moduleName, "moduleName should not be null");

        ModuleConfig moduleConfig = moduleConfigMap.get(moduleName);
        Collection<ShardConfig> shardConfigs = moduleConfig != null ? moduleConfig.getShardConfigs() :
            Collections.<ShardConfig>emptySet();
        return !shardConfigs.isEmpty() ? shardConfigs.iterator().next().getName(): null;
    }

    @Nullable
    @Override
    public String getShardNameForPrefix(@Nonnull final YangInstanceIdentifier prefix) {
        Preconditions.checkNotNull(prefix, "prefix should not be null");

        YangInstanceIdentifier bestMatch = YangInstanceIdentifier.EMPTY;

        for (final YangInstanceIdentifier yid : prefixConfigMap.keySet()) {
            if (yid.contains(prefix) && yid.getPathArguments().size() > bestMatch.getPathArguments().size()) {
                bestMatch = yid;
            }
        }

        //TODO we really should have mapping based on prefix instead of Strings
        return ClusterUtils.getCleanShardName(prefixConfigMap.get(bestMatch).getPrefix().getRootIdentifier());
    }

    @Override
    public Collection<MemberName> getMembersFromShardName(final String shardName) {
        Preconditions.checkNotNull(shardName, "shardName should not be null");

        for(ModuleConfig moduleConfig: moduleConfigMap.values()) {
            ShardConfig shardConfig = moduleConfig.getShardConfig(shardName);
            if(shardConfig != null) {
                return shardConfig.getReplicas();
            }
        }

        for (final PrefixShardConfiguration prefixConfig : prefixConfigMap.values()) {
            if (shardName.equals(ClusterUtils.getCleanShardName(prefixConfig.getPrefix().getRootIdentifier()))) {
                return prefixConfig.getShardMemberNames();
            }
        }

        return Collections.emptyList();
    }

    @Override
    public Set<String> getAllShardNames() {
        return allShardNames;
    }

    @Override
    public Collection<MemberName> getUniqueMemberNamesForAllShards() {
        Set<MemberName> allNames = new HashSet<>();
        for(String shardName: getAllShardNames()) {
            allNames.addAll(getMembersFromShardName(shardName));
        }

        return allNames;
    }

    @Override
    public synchronized void addModuleShardConfiguration(ModuleShardConfiguration config) {
        Preconditions.checkNotNull(config, "ModuleShardConfiguration should not be null");

        ModuleConfig moduleConfig = ModuleConfig.builder(config.getModuleName()).
                nameSpace(config.getNamespace().toASCIIString()).
                shardStrategy(createShardStrategy(config.getModuleName(), config.getShardStrategyName())).
                shardConfig(config.getShardName(), config.getShardMemberNames()).build();

        updateModuleConfigMap(moduleConfig);

        namespaceToModuleName = ImmutableMap.<String, String>builder().putAll(namespaceToModuleName).
                put(moduleConfig.getNameSpace(), moduleConfig.getName()).build();
        allShardNames = ImmutableSet.<String>builder().addAll(allShardNames).add(config.getShardName()).build();
    }

    @Override
    public void addPrefixShardConfiguration(final PrefixShardConfiguration config) {
        Preconditions.checkNotNull(config, "PrefixShardConfiguration cannot be null");
        updatePrefixConfigMap(config);
        allShardNames = ImmutableSet.<String>builder().addAll(allShardNames).add(ClusterUtils.getCleanShardName(config.getPrefix().getRootIdentifier())).build();
    }

    private void updatePrefixConfigMap(final PrefixShardConfiguration config) {
        final Map<YangInstanceIdentifier, PrefixShardConfiguration> newPrefixConfigMap = new HashMap<>(prefixConfigMap);
        newPrefixConfigMap.put(config.getPrefix().getRootIdentifier(), config);
        prefixConfigMap = ImmutableMap.copyOf(newPrefixConfigMap);
    }

    private ShardStrategy createShardStrategy(String moduleName, String shardStrategyName) {
        return ShardStrategyFactory.newShardStrategyInstance(moduleName, shardStrategyName, this);
    }

    @Override
    public boolean isShardConfigured(String shardName) {
        Preconditions.checkNotNull(shardName, "shardName should not be null");
        return allShardNames.contains(shardName);
    }

    @Override
    public void addMemberReplicaForShard (String shardName, MemberName newMemberName) {
        Preconditions.checkNotNull(shardName, "shardName should not be null");
        Preconditions.checkNotNull(newMemberName, "MemberName should not be null");

        for(ModuleConfig moduleConfig: moduleConfigMap.values()) {
            ShardConfig shardConfig = moduleConfig.getShardConfig(shardName);
            if(shardConfig != null) {
                Set<MemberName> replicas = new HashSet<>(shardConfig.getReplicas());
                replicas.add(newMemberName);
                updateModuleConfigMap(ModuleConfig.builder(moduleConfig).shardConfig(shardName, replicas).build());
                return;
            }
        }
    }

    @Override
    public void removeMemberReplicaForShard (String shardName, MemberName newMemberName) {
        Preconditions.checkNotNull(shardName, "shardName should not be null");
        Preconditions.checkNotNull(newMemberName, "MemberName should not be null");

        for(ModuleConfig moduleConfig: moduleConfigMap.values()) {
            ShardConfig shardConfig = moduleConfig.getShardConfig(shardName);
            if(shardConfig != null) {
                Set<MemberName> replicas = new HashSet<>(shardConfig.getReplicas());
                replicas.remove(newMemberName);
                updateModuleConfigMap(ModuleConfig.builder(moduleConfig).shardConfig(shardName, replicas).build());
                return;
            }
        }
    }

    @Override
    public ShardStrategy getStrategyForPrefix(@Nonnull final YangInstanceIdentifier prefix) {
        Preconditions.checkNotNull(prefix, "Prefix cannot be null");
        // FIXME using prefix tables like in mdsal will be better
        YangInstanceIdentifier lastFound = YangInstanceIdentifier.EMPTY;

        for (final YangInstanceIdentifier yid : prefixConfigMap.keySet()) {
            if (yid.contains(prefix) && yid.getPathArguments().size() > lastFound.getPathArguments().size()) {
                lastFound = yid;
            }
        }

        final PrefixShardConfiguration configuration = prefixConfigMap.get(lastFound);
        if (configuration == null) {
            return null;
        }
        return new PrefixShardStrategy(ClusterUtils.getCleanShardName(configuration.getPrefix().getRootIdentifier()), this);
    }

    private void updateModuleConfigMap(final ModuleConfig moduleConfig) {
        final Map<String, ModuleConfig> newModuleConfigMap = new HashMap<>(moduleConfigMap);
        newModuleConfigMap.put(moduleConfig.getName(), moduleConfig);
        moduleConfigMap = ImmutableMap.copyOf(newModuleConfigMap);
    }
}
