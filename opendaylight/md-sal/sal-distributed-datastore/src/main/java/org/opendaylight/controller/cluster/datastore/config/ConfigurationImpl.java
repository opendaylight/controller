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
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.shardstrategy.PrefixShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

// TODO clean this up once we get rid of module based configuration, prefix one should be alot simpler
public class ConfigurationImpl implements Configuration {
    private volatile Map<String, ModuleConfig> moduleConfigMap;

    // TODO should this be initialized with something? on restart we should restore the shards from configuration?
    private volatile Map<DOMDataTreeIdentifier, PrefixShardConfiguration> prefixConfigMap = Collections.emptyMap();

    // Look up maps to speed things up

    private volatile Map<String, String> namespaceToModuleName;
    private volatile Set<String> allShardNames;

    public ConfigurationImpl(final String moduleShardsConfigPath, final String modulesConfigPath) {
        this(new FileModuleShardConfigProvider(moduleShardsConfigPath, modulesConfigPath));
    }

    public ConfigurationImpl(final ModuleShardConfigProvider provider) {
        ImmutableMap.Builder<String, ModuleConfig> mapBuilder = ImmutableMap.builder();
        for (Map.Entry<String, ModuleConfig.Builder> e: provider.retrieveModuleConfigs(this).entrySet()) {
            mapBuilder.put(e.getKey(), e.getValue().build());
        }

        this.moduleConfigMap = mapBuilder.build();

        this.allShardNames = createAllShardNames(moduleConfigMap.values());
        this.namespaceToModuleName = createNamespaceToModuleName(moduleConfigMap.values());
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
        requireNonNull(nameSpace, "nameSpace should not be null");

        return namespaceToModuleName.get(nameSpace);
    }

    @Override
    public ShardStrategy getStrategyForModule(final String moduleName) {
        requireNonNull(moduleName, "moduleName should not be null");

        ModuleConfig moduleConfig = moduleConfigMap.get(moduleName);
        return moduleConfig != null ? moduleConfig.getShardStrategy() : null;
    }

    @Override
    public String getShardNameForModule(final String moduleName) {
        requireNonNull(moduleName, "moduleName should not be null");
        ModuleConfig moduleConfig = moduleConfigMap.get(moduleName);
        if (moduleConfig != null) {
            Collection<ShardConfig> shardConfigs = moduleConfig.getShardConfigs();
            if (!shardConfigs.isEmpty()) {
                return shardConfigs.iterator().next().getName();
            }
        }

        return null;
    }

    @Override
    public String getShardNameForPrefix(final DOMDataTreeIdentifier prefix) {
        requireNonNull(prefix, "prefix should not be null");

        Entry<DOMDataTreeIdentifier, PrefixShardConfiguration> bestMatchEntry = new SimpleEntry<>(
                new DOMDataTreeIdentifier(prefix.getDatastoreType(), YangInstanceIdentifier.empty()), null);

        for (Entry<DOMDataTreeIdentifier, PrefixShardConfiguration> entry : prefixConfigMap.entrySet()) {
            if (entry.getKey().contains(prefix) && entry.getKey().getRootIdentifier().getPathArguments().size()
                    > bestMatchEntry.getKey().getRootIdentifier().getPathArguments().size()) {
                bestMatchEntry = entry;
            }
        }

        //TODO we really should have mapping based on prefix instead of Strings
        return ClusterUtils.getCleanShardName(bestMatchEntry.getKey().getRootIdentifier());
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

        for (final PrefixShardConfiguration prefixConfig : prefixConfigMap.values()) {
            if (shardName.equals(ClusterUtils.getCleanShardName(prefixConfig.getPrefix().getRootIdentifier()))) {
                return prefixConfig.getShardMemberNames();
            }
        }

        return Collections.emptyList();
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
                .nameSpace(config.getNamespace().toASCIIString())
                .shardStrategy(createShardStrategy(config.getModuleName(), config.getShardStrategyName()))
                .shardConfig(config.getShardName(), config.getShardMemberNames()).build();

        updateModuleConfigMap(moduleConfig);

        namespaceToModuleName = ImmutableMap.<String, String>builder().putAll(namespaceToModuleName)
                .put(moduleConfig.getNamespace(), moduleConfig.getName()).build();
        allShardNames = ImmutableSet.<String>builder().addAll(allShardNames).add(config.getShardName()).build();
    }

    @Override
    public void addPrefixShardConfiguration(final PrefixShardConfiguration config) {
        addPrefixConfig(requireNonNull(config, "PrefixShardConfiguration cannot be null"));
        allShardNames = ImmutableSet.<String>builder().addAll(allShardNames)
                .add(ClusterUtils.getCleanShardName(config.getPrefix().getRootIdentifier())).build();
    }

    @Override
    public void removePrefixShardConfiguration(final DOMDataTreeIdentifier prefix) {
        removePrefixConfig(requireNonNull(prefix, "Prefix cannot be null"));

        final HashSet<String> temp = new HashSet<>(allShardNames);
        temp.remove(ClusterUtils.getCleanShardName(prefix.getRootIdentifier()));

        allShardNames = ImmutableSet.copyOf(temp);
    }

    @Override
    public Map<DOMDataTreeIdentifier, PrefixShardConfiguration> getAllPrefixShardConfigurations() {
        return ImmutableMap.copyOf(prefixConfigMap);
    }

    private void addPrefixConfig(final PrefixShardConfiguration config) {
        final Map<DOMDataTreeIdentifier, PrefixShardConfiguration> newPrefixConfigMap = new HashMap<>(prefixConfigMap);
        newPrefixConfigMap.put(config.getPrefix(), config);
        prefixConfigMap = ImmutableMap.copyOf(newPrefixConfigMap);
    }

    private void removePrefixConfig(final DOMDataTreeIdentifier prefix) {
        final Map<DOMDataTreeIdentifier, PrefixShardConfiguration> newPrefixConfigMap = new HashMap<>(prefixConfigMap);
        newPrefixConfigMap.remove(prefix);
        prefixConfigMap = ImmutableMap.copyOf(newPrefixConfigMap);
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

    @Override
    public ShardStrategy getStrategyForPrefix(final DOMDataTreeIdentifier prefix) {
        requireNonNull(prefix, "Prefix cannot be null");
        // FIXME using prefix tables like in mdsal will be better
        Entry<DOMDataTreeIdentifier, PrefixShardConfiguration> bestMatchEntry = new SimpleEntry<>(
                new DOMDataTreeIdentifier(prefix.getDatastoreType(), YangInstanceIdentifier.empty()), null);

        for (Entry<DOMDataTreeIdentifier, PrefixShardConfiguration> entry : prefixConfigMap.entrySet()) {
            if (entry.getKey().contains(prefix) && entry.getKey().getRootIdentifier().getPathArguments().size()
                    > bestMatchEntry.getKey().getRootIdentifier().getPathArguments().size()) {
                bestMatchEntry = entry;
            }
        }

        if (bestMatchEntry.getValue() == null) {
            return null;
        }
        return new PrefixShardStrategy(ClusterUtils
                .getCleanShardName(bestMatchEntry.getKey().getRootIdentifier()),
                bestMatchEntry.getKey().getRootIdentifier());
    }

    private void updateModuleConfigMap(final ModuleConfig moduleConfig) {
        final Map<String, ModuleConfig> newModuleConfigMap = new HashMap<>(moduleConfigMap);
        newModuleConfigMap.put(moduleConfig.getName(), moduleConfig);
        moduleConfigMap = ImmutableMap.copyOf(newModuleConfigMap);
    }
}
