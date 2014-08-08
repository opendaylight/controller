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
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ModuleShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationImpl implements Configuration {

    private final List<ModuleShard> moduleShards = new ArrayList<>();

    private final List<Module> modules = new ArrayList<>();

    private static final Logger
        LOG = LoggerFactory.getLogger(DistributedDataStore.class);

    // Look up maps to speed things up

    // key = memberName, value = list of shardNames
    private Map<String, List<String>> memberShardNames = new HashMap<>();

    // key = shardName, value = list of replicaNames (replicaNames are the same as memberNames)
    private Map<String, List<String>> shardReplicaNames = new HashMap<>();


    public ConfigurationImpl(String moduleShardsConfigPath,

        String modulesConfigPath){

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

        readModuleShards(moduleShardsConfig);

        readModules(modulesConfig);
    }

    @Override public List<String> getMemberShardNames(String memberName){

        Preconditions.checkNotNull(memberName, "memberName should not be null");

        if(memberShardNames.containsKey(memberName)){
            return memberShardNames.get(memberName);
        }

        List<String> shards = new ArrayList();
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

    @Override public Optional<String> getModuleNameFromNameSpace(String nameSpace) {

        Preconditions.checkNotNull(nameSpace, "nameSpace should not be null");

        for(Module m : modules){
            if(m.getNameSpace().equals(nameSpace)){
                return Optional.of(m.getName());
            }
        }
        return Optional.absent();
    }

    @Override public Map<String, ShardStrategy> getModuleNameToShardStrategyMap() {
        Map<String, ShardStrategy> map = new HashMap<>();
        for(Module m : modules){
            map.put(m.getName(), m.getShardStrategy());
        }
        return map;
    }

    @Override public List<String> getShardNamesFromModuleName(String moduleName) {

        Preconditions.checkNotNull(moduleName, "moduleName should not be null");

        for(ModuleShard m : moduleShards){
            if(m.getModuleName().equals(moduleName)){
                List<String> l = new ArrayList<>();
                for(Shard s : m.getShards()){
                    l.add(s.getName());
                }
                return l;
            }
        }

        return Collections.EMPTY_LIST;
    }

    @Override public List<String> getMembersFromShardName(String shardName) {

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
        shardReplicaNames.put(shardName, Collections.EMPTY_LIST);
        return Collections.EMPTY_LIST;
    }



    private void readModules(Config modulesConfig) {
        List<? extends ConfigObject> modulesConfigObjectList =
            modulesConfig.getObjectList("modules");

        for(ConfigObject o : modulesConfigObjectList){
            ConfigObjectWrapper w = new ConfigObjectWrapper(o);
            modules.add(new Module(w.stringValue("name"), w.stringValue(
                "namespace"), w.stringValue("shard-strategy")));
        }
    }

    private void readModuleShards(Config moduleShardsConfig) {
        List<? extends ConfigObject> moduleShardsConfigObjectList =
            moduleShardsConfig.getObjectList("module-shards");

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

            this.moduleShards.add(new ModuleShard(moduleName, shards));
        }
    }


    private class ModuleShard {
        private final String moduleName;
        private final List<Shard> shards;

        public ModuleShard(String moduleName, List<Shard> shards) {
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

    private class Shard {
        private final String name;
        private final List<String> replicas;

        Shard(String name, List<String> replicas) {
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

        Module(String name, String nameSpace, String shardStrategy) {
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

        ConfigObjectWrapper(ConfigObject configObject){
            this.configObject = configObject;
        }

        public String stringValue(String name){
            return configObject.get(name).unwrapped().toString();
        }
    }
}
