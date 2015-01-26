/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ModuleShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationImpl implements Configuration {

    private final List<ModuleShard> moduleShards;

    private final List<Module> modules;

    private static final Logger
        LOG = LoggerFactory.getLogger(DistributedDataStore.class);

    // Look up maps to speed things up

    // key = memberName, value = list of shardNames
    private final Map<String, List<String>> memberShardNames = new HashMap<>();

    // key = shardName, value = list of replicaNames (replicaNames are the same as memberNames)
    private final Map<String, List<String>> shardReplicaNames = new HashMap<>();

    private final Map<String, String> namespaceToModuleName;

    public ConfigurationImpl(final String moduleShardsConfigPath,

        final String modulesConfigPath){

        Preconditions.checkNotNull(moduleShardsConfigPath, "moduleShardsConfigPath should not be null");
        Preconditions.checkNotNull(modulesConfigPath, "modulesConfigPath should not be null");


        File moduleShardsFile = new File("./configuration/initial/" + moduleShardsConfigPath);
        File modulesFile = new File("./configuration/initial/" + modulesConfigPath);

        Config moduleShardsConfig = null;
        if(moduleShardsFile.exists()) {
            LOG.info("module shards config file exists - reading config from it");
            moduleShardsConfig = ConfigFactory.parseFile(moduleShardsFile);
        } else {
            LOG.warn("module shards configuration read from resource");
            moduleShardsConfig = ConfigFactory.load(moduleShardsConfigPath);
        }

        Config modulesConfig = null;
        if(modulesFile.exists()) {
            LOG.info("modules config file exists - reading config from it");
            modulesConfig = ConfigFactory.parseFile(modulesFile);
        } else {
            LOG.warn("modules configuration read from resource");
            modulesConfig = ConfigFactory.load(modulesConfigPath);
        }

        this.moduleShards = readModuleShards(moduleShardsConfig);
        this.modules = readModules(modulesConfig);

        namespaceToModuleName = createNamespaceToModuleName(modules);
    }

    private static Map<String, String> createNamespaceToModuleName(Iterable<Module> modules) {
        final com.google.common.collect.ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
        for (Module m : modules) {
            b.put(m.getNameSpace(), m.getName());
        }
        return b.build();
    }

    @Override public List<String> getMemberShardNames(final String memberName){

        Preconditions.checkNotNull(memberName, "memberName should not be null");

        if(memberShardNames.containsKey(memberName)){
            return memberShardNames.get(memberName);
        }

        List<String> shards = new ArrayList<>();
        for(ModuleShard ms : moduleShards){
            for(Shard s : ms.getShards()){
                for(String m : s.getReplicas()){
                    if(memberName.equals(m)){
                        shards.add(s.getName());
                    }
                }
            }
        }

        memberShardNames.put(memberName, shards);

        return shards;

    }

    @Override
    public Optional<String> getModuleNameFromNameSpace(final String nameSpace) {
        Preconditions.checkNotNull(nameSpace, "nameSpace should not be null");
        return Optional.fromNullable(namespaceToModuleName.get(nameSpace));
    }

    @Override public Map<String, ShardStrategy> getModuleNameToShardStrategyMap() {
        // FIXME: can be constant view of modules
        Map<String, ShardStrategy> map = new HashMap<>();
        for(Module m : modules){
            map.put(m.getName(), m.getShardStrategy());
        }
        return map;
    }

    @Override public List<String> getShardNamesFromModuleName(final String moduleName) {

        Preconditions.checkNotNull(moduleName, "moduleName should not be null");

        // FIXME: can be constant view of moduleShards
        for(ModuleShard m : moduleShards){
            if(m.getModuleName().equals(moduleName)){
                List<String> l = new ArrayList<>();
                for(Shard s : m.getShards()){
                    l.add(s.getName());
                }
                return l;
            }
        }

        return Collections.emptyList();
    }

    @Override public List<String> getMembersFromShardName(final String shardName) {

        Preconditions.checkNotNull(shardName, "shardName should not be null");

        if(shardReplicaNames.containsKey(shardName)){
            return shardReplicaNames.get(shardName);
        }

        for(ModuleShard ms : moduleShards){
            for(Shard s : ms.getShards()) {
                if(s.getName().equals(shardName)){
                    List<String> replicas = s.getReplicas();
                    shardReplicaNames.put(shardName, replicas);
                    return replicas;
                }
            }
        }
        shardReplicaNames.put(shardName, Collections.<String>emptyList());
        return Collections.emptyList();
    }

    @Override public Set<String> getAllShardNames() {
        Set<String> shardNames = new LinkedHashSet<>();
        for(ModuleShard ms : moduleShards){
            for(Shard s : ms.getShards()) {
                shardNames.add(s.getName());
            }
        }
        return shardNames;
    }



    private List<Module> readModules(final Config modulesConfig) {
        List<? extends ConfigObject> modulesConfigObjectList =
            modulesConfig.getObjectList("modules");

        final Builder<Module> b = ImmutableList.builder();
        for(ConfigObject o : modulesConfigObjectList){
            ConfigObjectWrapper w = new ConfigObjectWrapper(o);
            b.add(new Module(w.stringValue("name"), w.stringValue(
                "namespace"), w.stringValue("shard-strategy")));
        }

        return b.build();
    }

    private static List<ModuleShard> readModuleShards(final Config moduleShardsConfig) {
        List<? extends ConfigObject> moduleShardsConfigObjectList =
            moduleShardsConfig.getObjectList("module-shards");

        final Builder<ModuleShard> b = ImmutableList.builder();
        for(ConfigObject moduleShardConfigObject : moduleShardsConfigObjectList){

            String moduleName = moduleShardConfigObject.get("name").unwrapped().toString();

            List<? extends ConfigObject> shardsConfigObjectList =
                moduleShardConfigObject.toConfig().getObjectList("shards");

            List<Shard> shards = new ArrayList<>();

            for(ConfigObject shard : shardsConfigObjectList){
                String shardName = shard.get("name").unwrapped().toString();
                List<String> replicas = shard.toConfig().getStringList("replicas");
                shards.add(new Shard(shardName, replicas));
            }

            b.add(new ModuleShard(moduleName, shards));
        }

        return b.build();
    }


    private static class ModuleShard {
        private final String moduleName;
        private final List<Shard> shards;

        public ModuleShard(final String moduleName, final List<Shard> shards) {
            this.moduleName = moduleName;
            this.shards = shards;
        }

        public String getModuleName() {
            return moduleName;
        }

        public List<Shard> getShards() {
            return shards;
        }
    }

    private static class Shard {
        private final String name;
        private final List<String> replicas;

        Shard(final String name, final List<String> replicas) {
            this.name = name;
            this.replicas = replicas;
        }

        public String getName() {
            return name;
        }

        public List<String> getReplicas() {
            return replicas;
        }
    }

    private class Module {

        private final String name;
        private final String nameSpace;
        private final ShardStrategy shardStrategy;

        Module(final String name, final String nameSpace, final String shardStrategy) {
            this.name = name;
            this.nameSpace = nameSpace;
            if(ModuleShardStrategy.NAME.equals(shardStrategy)){
                this.shardStrategy = new ModuleShardStrategy(name, ConfigurationImpl.this);
            } else {
                this.shardStrategy = new DefaultShardStrategy();
            }
        }

        public String getName() {
            return name;
        }

        public String getNameSpace() {
            return nameSpace;
        }

        public ShardStrategy getShardStrategy() {
            return shardStrategy;
        }
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
